# Design

## Context

摊主模式（`pages/vendor/index.vue`）已有商品上下架开关与订单队列；`StallVendorServiceImpl.listAllProducts()` 返回原始 `PmsProduct`（stock 未扣 SKU lockStock）。tabBar 为 `pages.json` 静态三栏（今日菜单/我的订单/我的），小程序无法动态删项。营收数据分散在 `oms_order`（order_type=摊位，pay_amount，status）与 `oms_order_item`（product_name/product_quantity/real_amount）。会员/摊主会话类型可从 `useMemberStore().authType`（'member' | 'admin'）判断。详见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 摊主会话 tabBar 仅剩「我的」，其余会话三栏不变
- 摊主商品列表展示真实剩余与售罄标识
- 今日战绩独立页：实收口径总额/订单数/商品明细

**Non-Goals:**

- 不做历史日期统计（仅"今天"）、不做图表可视化
- 不改顾客端 tabBar 行为与页面
- 不做库存修改功能（只读展示）
- 不做营收数据导出

## Decisions

**D1. tabBar：摊主会话隐藏 tabBar（hideTabBar 方案）；custom-tab-bar 已尝试并放弃。**
微信端 `custom-tab-bar` 原生组件方案已实现并经两轮真机验证失败：首版 `cover-view` 非法嵌套导致页面点击失效与切换卡顿，修正为普通 `view` 后点击仍完全失效。因无真机 console 报错无法继续定位 native 组件渲染问题，放弃该路径。实现改为：

- 摊主（管理员）会话：页面生命周期中调用 `uni.hideTabBar()` 隐藏整个底部 tabBar
- 会员/游客会话：`uni.showTabBar()` 保持三栏
- 摊主页头部提供「我的」入口（`uni.switchTab` 至 `/pages/user/user`，进入后 tabBar 保持隐藏直至退出摊主）
- 实现要点：tab 页与摊主页 `onShow` 按当前会话类型幂等调用 hide/show；调用失败静默（不影响页面功能）；会话判定复用 `readAuthSession()`（Vue 上下文内可用 Pinia store，无需原生组件的 storage 直读）

选 hideTabBar 而非自定义组件的原因：纯 API 调用无 native 渲染风险，且"摊主模式下没有底栏"比"只剩一项的底栏"更贴合摊主的使用方式（摊主页即唯一中心）。

**D2. 摊主商品接口复用 realStock 口径覆写 stock。**
`listAllProducts()` 遍历商品时计算 `sku.stock - lockStock`（无 SKU 用 product.stock）并覆写返回对象的 stock 字段——与 `StallMenuServiceImpl`（upgrade-home-ui D1/D2）完全同款，抽公共私有方法各自持有（两处实现都很短，不强行抽象公共类）。前端商品行加「剩余 N」文本，`stock<=0` 显示「售罄」。

**D3. 今日战绩接口一次聚合返回，服务端按状态过滤。**
`GET /stall/vendor/todayStats` 返回 `{ totalAmount, orderCount, items: [{ productName, quantity, amount }] }`。实现：查今日创建、order_type=摊位、status IN (1,2,3,4) 的订单（PageHelper 不分页，直接 list），汇总 pay_amount 与订单数；订单明细按 order_id in 批量查 oms_order_item，按 product_name 分组聚合 quantity 与 real_amount，按 amount 降序。日期边界用当日 00:00（服务器时区）。amount 以 BigDecimal 返回。备选（SQL GROUP BY 聚合）被否：订单量级为摊位日单量（几十到几百），内存聚合更直观且复用现有 Example 查询模式。

**D4. 今日战绩为独立页面，从摊主页头部进入。**
`pages/vendor/sales.vue`（navigationBarTitleText「今日战绩」），摊主页头部加「今日战绩」按钮 `navigateTo` 进入。页面结构：顶部两个统计卡（今日总额、订单数）+ 商品明细列表（名称、x N、金额）。`onShow` 加载 + `enablePullDownRefresh` 下拉刷新。接口 401/403 时跳回摊主页。

**D5. custom-tab-bar 的会话判定读 storage 会话，不引 Pinia。**
原生组件（非 Vue 上下文）无法用 Pinia，直接 `uni.getStorageSync('authSession')` 判 `type === 'admin'`（与 `readAuthSession` 同 key；组件内做轻量解析即可，不整包复用 auth-session）。登录/退出后跳转 tab 页时 `getTabBar().init()` 触发重渲染。

## Risks / Trade-offs

- [custom-tab-bar 真机渲染异常导致全局点击失效] → 已发生并按 D1 放弃该方案；hideTabBar 为纯 API 调用，无 native 渲染面，风险闭环。
- [hideTabBar/showTabBar 调用时机与幂等] → 统一在各页 onShow 按会话类型调用；重复调用无副作用；调用失败静默不影响页面功能。
- [今日统计的"今日"跨天边界] → 服务器时区自然日；摆摊场景日切后再看是昨日数据属预期，不引入"营业日"概念。
- [实收口径与退款] → 当前无线上退款流（摊主线下处理），实收口径不扣退款；若未来加退款需重审口径。
- [聚合接口无分页] → 日单量级小；明细按订单 id in 一次查询 + 内存分组，无性能风险。

## Migration Plan

后端（纯新增 + 返回语义修正）可先行；前端 tabBar 改造与摊主页/战绩页随后发布。回滚：前端回退即可恢复静态 tabBar；custom-tab-bar 目录删除不影响其他页面。

## Open Questions

（无——tabBar 链路风险由前置 spike 消解）
