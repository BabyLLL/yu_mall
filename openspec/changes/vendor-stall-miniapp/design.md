# Design

## Context

mall-app-web（uni-app Vue3 + TS 微信小程序）当前为标准电商形态，后端为 mall 微服务（mall-portal 提供小程序 API，Spring Boot + MyBatis + Redis + RabbitMQ）。已确认的关键现状：

- 订单表 `oms_order.status` 复用电商语义（0 待付款/1 待发货/2 已发货/3 已完成/4 已关闭/5 无效），`OmsPortalOrderServiceImpl` 已有 generateOrder、paySuccess、延迟队列超时取消（RabbitMQ 死信）。
- 下单强依赖 `OrderParam.memberReceiveAddressId` + cartIds（经 CartPromotionItem 优惠计算）。
- 小程序已有隐藏管理员体系：`/sso/miniapp/admin/login`（ums_admin 账号 + MiniappSessionService 单会话互斥，authType=admin），前端 `apis/member.ts` 已有 `adminLoginAPI`。mall-app-web 的 openspec 下存在空壳 change `hidden-vendor-mode-entry`（仅空 specs 目录，无内容，可忽略/清理）。
- 菜单数据源：`pms_product.publish_status=1` 已被 HomeDao / PmsPortalProductServiceImpl.search 用作上架过滤；`/home/**`、`/sso/**` 在 secure.ignored 白名单。
- Redis 已接入（验证码、miniapp session 前缀）。

## Goals / Non-Goals

**Goals:**

- 顾客 2 次点击下单：菜单点"+"→ 底部条"去下单"→ 确认页提交。
- 订单带按天流水取货码，摊位状态机驱动。
- 摊主在小程序内完成收款确认、制作推进、商品隐藏。

**Non-Goals:**

- 不做微信支付/订阅消息推送（口头叫号 + 顾客自查）。
- 不动 mall-admin-web、mall-search、支付宝模块。
- 不做品类扩展的数据模型（菜单网格天然可容纳新品类，届时只是加商品）。
- 不做多摊位/门店概念（单摊位）。

## Decisions

### D1: 状态机复用 oms_order.status 数值段，语义重映射

摊位状态映射：0=已下单(待付款)、1=已付款、2=制作中、3=待取货、4=已完成、5=已取消、6=已关闭(超时取消归入 5，原 4 语义弃用)。

- **理由**：避免迁移历史数据、不改表结构核心列，现有延迟取消任务（updateOrderStatus(ids, 4)）只需改目标值为 5 即可继续复用；顾客端/摊主端只看映射后的展示文案。
- **替代方案**：新增独立摊位订单表。否——两套订单表长期维护成本高，且会员维度订单列表要合并查询。
- **注意**：`confirmReceiveOrder`（原 2→3）对摊位订单无意义，前端不再暴露；后端保留不动。

### D2: 取货码用 Redis 自增，key 按天

`INCR stall:pickup:code:yyyyMMdd`，格式化为 `N%03d`，同时写入 `oms_order.pickup_code`（新增 varchar 列，mall-mbg 模型同步）。key 过期 48h 兜底清理。

- **理由**：单摊位并发量极低，Redis INCR 天然原子且实现最小；按天 key 自动实现"每日重置"。
- **替代方案**：查当天 max(pickup_code)+1（并发不安全，需加锁）；单独序号表（过重）。

### D3: 新增独立摊位接口族，不改造旧接口

`/stall/menu/list`（白名单）、`/stall/order/create`、`/stall/order/list`、`/stall/order/{id}/cancel`（会员）；`/stall/vendor/product/hide|show`、`/stall/vendor/order/{id}/confirmPay|startMaking|finishMaking|cancel`（摊主）。新建 `StallOrderService` 复用 `OmsPortalOrderServiceImpl` 的库存锁定/释放私有逻辑（提为共享或复制最小实现）。

- **理由**：generateOrder 与优惠/购物车/地址耦合深（CartPromotionItem 链路），原地瘦身风险大且会破坏旧接口；独立接口族让旧路径原样保留、新路径行为清晰。
- **替代方案**：改造 generateOrder 增加轻量模式。否——OrderParam 与 CartPromotionItem 的优惠计算链路牵一发动全身。

### D4: 摊位下单直传商品，绕过购物车

入参 `[{productId, quantity}] + note`，服务端按 pms_product 现价直接计算，锁定库存逻辑复用。前端"已选"状态存 Pinia 本地 store（内存 + 可选持久化），不再走 oms_cart_item。

- **理由**：烤肠摊无跨设备购物车需求，购物车表链路（含促销计算）全部绕开。
- **注意**：旧购物车接口/页面代码保留不删（仅入口移除），降低回滚成本。

### D5: 前端页面策略——重写首页为菜单页，其余入口裁剪

- `pages/index/index.vue` 重写：菜单网格（图/名/价/+按钮）+ 底部常驻已选条 + 已选弹层。tabBar 收敛为 2 个：今日菜单、我的订单。
- 新增页面：`pages/stall/confirm-order`（确认下单）、`pages/stall/order-success`（取货码大字）、`pages/vendor/index`（摊主模式：商品开关 + 订单队列两个分段）。
- 保留但移除入口：category、cart、coupon、brand、address、money/pay、product 详情（菜单加单不需要详情页；商品详情页代码保留以备扩展）。
- user 页保留登录/设置 + 隐藏入口。

### D6: 摊主会话复用现有 admin 登录链路

`adminLoginAPI` + MiniappSessionService（authType=admin，与 member 会话互斥）已可用，无需后端改动。摊主接口鉴权：JwtAuthenticationTokenFilter 已校验 authType 与 `miniapp.auth.expected-type=member` 的关系——摊主专用接口需放行 authType=admin 的请求，通过 `@PreAuthorize("hasAuthority('MINIAPP_ADMIN')")` 或配置层 expected-type 按路径区分（实现时确认 filter 对 admin claim 的放行细节，必要时在 mall-security 增加路径级 expected-type 配置）。

- **替代方案**：独立摊主口令硬编码。否——探索中确认采用账号体系（ums_admin），口令方案已弃。

### D7: 超时取消策略

复用 RabbitMQ 延迟消息：下单时发延迟消息（超时时长建议改为可配置，摊位场景默认 30 分钟），消费者将未付款订单置为已取消(5)并释放库存。与现有 CancelOrderSender/Receiver 结构一致，仅状态值与超时参数调整。

## Risks / Trade-offs

- [status 语义重映射与历史数据混淆] → 摊位上线前订单量少；映射集中在常量类，列表查询按新语义过滤；如已有历史电商订单，列表默认只查摊位来源（sourceType 或 orderType 标记新单，见 Open Questions）。
- [摊主接口鉴权路径与现有 expected-type=member 全局配置冲突] → 实现第一步先做鉴权 spike：验证 admin token 调用新接口的放行方式，再铺开接口。
- [菜单页无详情页，商品描述信息缺失] → 烤肠摊商品简单，卡片信息足够；后续品类复杂再恢复详情页入口。
- [删除页面入口但保留代码，pages.json 清理不当导致构建报错] → tasks 中单独列出 pages.json 与 tabBar 修改及构建验证步骤。
- [口头叫号可能漏单] → 取货码大字常驻 + 订单队列按序排列缓解；推送为明确的 Non-Goal，如后续需要再加订阅消息。

## Migration Plan

1. 数据库：`ALTER TABLE oms_order ADD COLUMN pickup_code varchar(16) NULL`（加列无破坏）。
2. 后端：部署含 stall 接口的新版本 mall-portal（旧接口不动，白名单追加 /stall/menu/**）。
3. 前端：发布新版小程序（审核期间旧版可用旧接口，互不影响）。
4. 回滚：前端回退旧版本即可；后端 stall 接口为增量，不回滚也无副作用。

## Open Questions

- 新订单是否需要独立标记（orderType=2 摊位单 或复用 sourceType）以便订单列表区分历史电商单？→ 倾向 orderType 新增枚举值 2=摊位订单，实现时确认对现有查询的影响面。
- 取货码是否需要防伪/校验场景（顾客报号取货时摊主核对订单）？→ 队列页按取货码展示即可覆盖，暂不做扫码核对。
