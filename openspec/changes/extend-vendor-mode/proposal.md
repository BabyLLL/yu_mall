# Proposal

## Why

摊主模式目前只有商品上下架开关与订单队列推进：摊主看到的库存是未扣锁定库存的"假库存"，无法知道真实还能卖多少；也没有任何营收视角——摊主收了一天的钱，小程序里看不到今天卖了多少、卖了什么；且摊主会话下底部 tabBar 仍显示顾客侧的「今日菜单」「我的订单」，界面身份混乱。

## What Changes

- **摊主会话 tabBar 隐藏**：摊主（管理员）会话下隐藏底部 tabBar（`hideTabBar`），不再展示顾客侧的「今日菜单」「我的订单」；摊主页提供「我的」入口；会员/游客会话三栏不变。（自定义 tabBar 组件方案已尝试并因真机渲染问题放弃，见 design D1）
- **摊主端真实库存**：后端摊主商品接口返回真实剩余库存（扣锁定库存，与顾客菜单接口同款口径），商品管理列表每行显示「剩余 N」，售罄商品标识展示。
- **今日战绩独立页面**：新页 `pages/vendor/sales.vue`，从摊主页进入；后端新增摊主今日统计接口，实收口径（今日创建且状态为已付款/制作中/待取货/已完成，排除未付款与已取消）：今日总额、订单数、商品明细（名称 x 数量 x 金额）。`onShow` 刷新 + 下拉刷新。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

> 说明：`vendor-mode` 能力的主 spec 尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），沿用项目现行约定，以 ADDED 需求形式补充，归档时与主 delta 一并合并。

- `vendor-mode`: 新增「摊主真实库存可见」（商品管理展示扣锁定库存后的真实剩余与售罄标识）、「今日战绩统计」（独立页面展示实收口径的今日总额/订单数/商品明细，仅摊主可访问）、「摊主会话 tabBar 定制」（摊主会话下 tabBar 仅显示「我的」，顾客会话不变）三个需求。

## Impact

- **后端**：`mall/mall-portal` — `StallVendorServiceImpl.listAllProducts()` 覆写真实剩余库存（复用 `StallMenuServiceImpl` 的 realStock 口径）；`StallVendorController` 新增 `GET /stall/vendor/todayStats`（聚合 `oms_order` + `oms_order_item`，摊位订单类型，按日与状态过滤）。
- **前端**：`mall-app-web/src` — 新增 `custom-tab-bar` 自定义 tabBar 组件（按会话渲染）+ `pages.json` 开启 `custom: true`；`pages/vendor/index.vue`（商品行显示剩余/售罄、今日战绩入口）；新建 `pages/vendor/sales.vue`；`apis/stall.ts` 新增统计接口与类型。
- **兼容性**：摊主商品接口仅改返回语义（stock 含义与顾客菜单接口对齐）且调用方只有摊主页；`todayStats` 为纯新增接口；自定义 tabBar 需回归验证三个 tab 页在会员/游客/摊主三种会话下的显示与跳转。
