# Proposal

## Why

mall-app-web 目前是标准电商小程序（分类/购物车/优惠券/收货地址/在线支付），下单链路长（详情→购物车→确认订单→支付，约 6 步）且强依赖收货地址，不符合"小摊售卖"场景。摊主实际业务是烤肠摊（后续可扩展品类）：顾客扫码进店、快速下单、排队叫号取货、当面付款。需要把前后端整体改造成摊位形态，让顾客 2 次点击完成下单。

## What Changes

- **首页改造（BREAKING）**：首页改为"今日菜单"三合一页面（菜单展示 = 选单 = 下单入口），商品卡片直接带"+"加单按钮，底部常驻已选条（件数/合计/去下单）。
- **下单流程（BREAKING）**：新确认下单页（已选清单 + 备注 + 提交），下单成功页展示大字取货码。砍掉购物车 tab、分类 tab、支付页、优惠券、品牌、收货地址、秒杀、分享返现、足迹/收藏/关注等入口与页面。
- **取货码**：下单后生成按天流水取货码（N001 起，每天重置），订单列表/详情常驻大字展示，不做推送，摊主口头叫号。
- **订单状态机（BREAKING）**：电商状态机（待付款→待发货→已发货→已完成/已关闭）改为摊位状态机：已下单→已付款→制作中→待取货→已完成，任一非完成态可取消。未付款超时自动取消沿用现有延迟队列。
- **当面付**：删除前端支付页面与支付跳转；订单"已付款"由摊主当面收款后在摊主模式手动确认。后端支付宝/支付回调代码保留不动。
- **下单接口瘦身（BREAKING）**：新增摊位下单接口，入参仅 商品+数量+备注，去掉收货地址、优惠券、积分、运费依赖。旧 generateOrder 接口保留但前端不再调用。
- **摊主模式**：复用已有小程序管理员登录体系（ums_admin 账号 + 隐藏入口），摊主在"我的"页隐藏入口输入账号密码进入摊主模式，可：一键隐藏/上架商品（复用 publish_status）、查看订单队列（待付款→点"确认收款"、已付款→点"开始制作"、制作中→点"出餐完成"）、取消订单。

## Capabilities

### New Capabilities

- `stall-menu-ordering`: 今日菜单首页与快速下单流程——菜单展示、加单、底部已选条、确认下单页、下单成功与取货码展示、顾客取消订单。
- `stall-order-lifecycle`: 摊位订单生命周期——摊位下单接口、取货码生成规则（按天流水）、摊位订单状态机与状态流转、未付款超时取消。
- `vendor-mode`: 摊主模式——隐藏入口与管理员登录、商品隐藏/上架开关、摊主订单队列操作（确认收款/开始制作/出餐完成/取消）。

### Modified Capabilities

（无——项目尚无已归档 spec，全部为新增能力。）

## Impact

- **后端 mall-portal**：新增 `StallOrderController`、`StallVendorController`、`StallMenuController`（或并入现有 controller）；新增取货码生成（Redis 自增，key 按天）；`OmsOrder` 状态语义重定义（影响现有 `OmsPortalOrderServiceImpl` 中 confirmReceiveOrder、paySuccess 等方法的语义对齐）；订单表新增 pickup_code 列（mall-mbg 模型与 mapper 同步）。`/sso/**`、`/home/**` 白名单已有，新增 `/stall/menu/**` 白名单。
- **前端 mall-app-web**：重写 `pages.json`（2 个 tab：今日菜单、我的订单）；首页 index.vue 重写为菜单页；新增确认下单页、下单成功页、摊主模式页面组；删除/移除入口：category、cart tab、product 详情复杂化部分（保留轻量详情或直接菜单加单）、money/pay、coupon、brand、address、user 下的足迹/收藏/关注。apis 层新增 stall 相关接口，废弃 cart/coupon/brand/address 调用。
- **数据库**：oms_order 新增 pickup_code 列（varchar），索引按 create_time+id 已可支撑按天流水查询。
- **不动的部分**：mall-admin-web 管理后台、支付宝模块、RabbitMQ 延迟取消机制、微信登录体系。
