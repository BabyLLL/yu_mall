# Tasks

## 1. 页面拆分

- [x] 1.1 新建 `pages/vendor/products.vue`：从 vendor/index 商品管理分段迁移列表 + 上下架开关（含失败回滚）+ 剩余库存/售罄标识；`pages.json` 注册；验证：构建通过，开关切换与失败回滚行为与迁移前一致
- [x] 1.2 新建 `pages/vendor/orders.vue`：从 vendor/index 订单队列分段迁移状态过滤 + 推进操作（确认收款/开始制作/出餐完成/取货完成）+ 取消确认；`pages.json` 注册；验证：构建通过，各状态推进与取消确认行为与迁移前一致

## 2. 工作台重构

- [x] 2.1 重构 `pages/vendor/index.vue` 为工作台：头部（我的/退出）+ 今日实收/今日订单双卡片（onShow 拉 `todayStats`，点卡片进 `sales.vue`）+ 商品管理/订单队列两张全宽入口卡片（右侧分别显示「M 个商品」「N 单进行中」，onShow 并行预拉）；移除分段逻辑与两段列表模板、移除头部旧「今日战绩」按钮；验证：`npm run tsc` + 构建通过，工作台无列表无分段
- [x] 2.2 `pages/vendor/sales.vue` 样式与工作台战绩卡保持同源（深色统计卡视觉一致），不改其数据逻辑；验证：构建通过，两页卡片观感一致

## 3. 我的页收敛

- [x] 3.1 `pages/user/user.vue`：order-section（全部订单/已下单）加 `v-if="!memberStore.isAdmin"`；验证：`npm run tsc` + 构建通过

## 4. 集成验证（需开发者工具 + 后端）

- [ ] 4.1 摊主全链路走查：登录摊主 → 我的页无订单按钮 → 进工作台见战绩卡与两入口 → 战绩卡进明细页返回数据刷新 → 商品管理开关/售罄正常 → 订单队列推进/取消正常 → 「我的」入口/退出正常；验证：开发者工具手工走查
- [ ] 4.2 会员/游客回归：「我的」页订单入口正常、下单看单全流程不受影响；验证：开发者工具手工走查

## Workflow follow-up

- 归档时与 `vendor-stall-miniapp`、`extend-vendor-mode` 的 `vendor-mode` delta 合并（本变更 ADDED 补充，无冲突需求）。
