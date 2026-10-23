# Design

## Context

摊主页 `pages/vendor/index.vue` 现为 header（今日战绩/我的/退出按钮）+ 分段切换（商品管理/订单队列同页）结构；战绩统计已有接口 `GET /stall/vendor/todayStats` 与明细页 `pages/vendor/sales.vue`；商品逻辑（`getVendorProductsAPI` + 上下架开关 + 剩余/售罄）与订单队列逻辑（`getVendorOrdersAPI` + 过滤 + 推进/取消）均在分段内实现，拆页即迁移。「我的」页 `pages/user/user.vue` 的 `order-section`（全部订单/已下单）当前对所有会话展示；会话类型可从 `useMemberStore().isAdmin` 判断。详见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 工作台 = 头部 + 战绩双卡 + 两张入口卡片，无列表无分段
- 商品管理 / 订单队列拆独立页，逻辑原样迁移
- 「我的」页摊主会话隐藏顾客订单入口

**Non-Goals:**

- 不改任何后端接口与数据结构
- 不动战绩明细页 `sales.vue` 的内容（仅进入路径变化）
- 不做下拉刷新等新交互（各页保持与迁移前一致的加载方式）
- 不改会员/游客视角的「我的」页

## Decisions

**D1. 工作台卡片布局：战绩双卡（可点）+ 两张全宽入口卡片。**
战绩卡沿用 `sales.vue` 的深色统计卡样式（视觉一致性）；两张入口卡为白底全宽卡片，右侧放辅助信息（商品管理→「M 个商品」、订单队列→「N 单进行中」）。辅助数据在 onShow 时随 `getVendorProductsAPI` / `getVendorOrdersAPI` 并行拉取（订单队列用 status=-1 首页数据在内存中计算进行中数量，不新增接口）。备选（不加辅助信息，纯箭头）被否：加一行字成本极低，可读性收益明显，符合"直观"。

**D2. 拆页迁移而非重写：products.vue / orders.vue 的逻辑从 vendor/index 分段中原样搬移。**
数据加载、开关回滚、状态推进、取消确认等函数逐段迁移，样式沿用摊主页现有色板（`$stall-red` 深红棕系，与工作台/战绩页一致）；迁移后删除 vendor/index 内的 segment 状态与两段模板。风险点仅在搬移遗漏，验证方式为逐功能走查（任务列出）。

**D3. 「我的」页按 `memberStore.isAdmin` 隐藏 order-section。**
`v-if="!memberStore.isAdmin"` 包住整个订单区（含两个按钮）；会员/游客路径零改动。不动 `handleNavToOrder` 的登录引导逻辑（会员路径继续使用）。

**D4. sales.vue 头部旧入口按钮随 vendor/index 重构一并移除，战绩卡片成为唯一入口。**
进入路径收敛为一处，避免双入口漂移；`sales.vue` 自身代码不动。

## Risks / Trade-offs

- [拆页搬移遗漏交互（如开关失败回滚）] → 迁移采取整函数搬运而非重写；任务按功能点列走查清单逐项确认。
- [工作台辅助信息需要额外请求] → 商品/订单列表本来就是进入子页必拉的数据，工作台预拉一份仅多一次并发请求，量级可忽略；不为此加聚合接口。
- [摊主会话隐藏订单入口后无法查顾客订单] → 摊主身份本不应混用顾客订单视图；若确需查看可退出摊主模式（会话互斥，退出即回会员态）。

## Migration Plan

纯前端改造，一次发版生效；新页注册 pages.json 后旧 vendor/index 分段代码同版移除，无中间态。回滚即回退版本。

## Open Questions

（无）
