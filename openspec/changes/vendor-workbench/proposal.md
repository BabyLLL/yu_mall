# Proposal

## Why

摊主模式页当前把商品管理与订单队列塞在同页分段切换里，今日战绩又要再点一次头部按钮才能看——摊主开摊期间最高频的三件事（看战绩、管商品、处理订单）都藏了一层；且「我的」页在摊主会话下仍展示顾客视角的「全部订单/已下单」快捷入口，身份混淆。

## What Changes

- **「我的」页摊主化收敛**：摊主（管理员）会话下隐藏「全部订单 / 已下单」顾客订单快捷入口；会员会话不变。
- **摊主工作台重构**：`pages/vendor/index.vue` 从"分段切换页"重构为简洁工作台——头部（我的/退出）+ 今日战绩双卡片（今日实收、今日订单数，`onShow` 实时拉取，点卡片进入战绩明细页）+ 「商品管理」「订单队列」两张独立入口卡片；移除分段切换与页内列表。
- **页面拆分**：商品管理（列表 + 上下架开关 + 剩余库存/售罄标识）拆至新页 `pages/vendor/products.vue`；订单队列（状态过滤 + 推进操作）拆至新页 `pages/vendor/orders.vue`；战绩明细入口从头部按钮改为战绩卡片点击（`sales.vue` 保留，移除头部旧按钮）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

> 说明：`vendor-mode` 的主 spec 尚未归档（仍在 `vendor-stall-miniapp`、`extend-vendor-mode` 变更的 delta 中），沿用项目现行约定以 ADDED 需求补充，归档时一并合并。

- `vendor-mode`: 新增「摊主工作台」需求——工作台布局（战绩卡片常驻、商品管理/订单队列独立入口）与「我的」页摊主会话隐藏顾客订单入口的规则。

## Impact

- **前端**：`mall-app-web/src` — `pages/vendor/index.vue`（重构为工作台）、新建 `pages/vendor/products.vue`、`pages/vendor/orders.vue`、`pages/user/user.vue`（摊主会话隐藏订单入口）、`pages.json`（注册两个新页）。
- **后端**：无改动（今日战绩复用 `GET /stall/vendor/todayStats`，商品/订单接口复用现有）。
- **兼容性**：`sales.vue` 的进入路径变化（头部按钮 → 战绩卡片），页面本身不动；既有库存/售罄展示与订单推进逻辑原样迁移到新页。
