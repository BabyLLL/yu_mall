# Tasks

## 1. Spike：自定义 tabBar 构建链路验证

- [x] 1.1 在 `src/custom-tab-bar/` 放置最小原生组件（固定渲染两个假 tab 项），`pages.json` 开启 `tabBar.custom: true`，执行 `npm run build:mp-weixin`，验证 `dist/build/mp-weixin/custom-tab-bar/` 存在且开发者工具预览中组件渲染、无「未找到 custom-tab-bar 组件」报错；验证：构建产物含 custom-tab-bar 目录 + 真机/工具目视
- [x] 1.2 若 spike 失败：在 design.md 附录记录失败原因，按回退方案（hideTabBar + 摊主页「我的」入口）重排后续 tabBar 任务；验证：design.md 附录已记录且任务清单已调整

## 2. 后端：真实库存 + 今日战绩接口

- [x] 2.1 `StallVendorServiceImpl.listAllProducts()` 覆写真实剩余库存（`sku.stock - lockStock`，无 SKU 用 `product.stock`，与 `StallMenuServiceImpl` 同口径）；验证：新增/更新单测覆盖「扣 lockStock」「无 SKU」「售罄返回 0」三断言，`mvn -pl mall-portal test -DskipTests=false -Dtest=Stall*` 全绿
- [x] 2.2 `StallVendorController` 新增 `GET /stall/vendor/todayStats`：今日创建、摊位订单类型、状态 IN(1,2,3,4)，返回 `{totalAmount, orderCount, items:[{productName, quantity, amount}]}`（明细按商品名分组聚合、金额降序）；验证：新增 `StallVendorServiceImplTest` 统计用例（含取消/未付款订单不计入），单测全绿

## 3. 前端：tabBar 会话隐藏

- [x] 3.1 移除 custom-tab-bar 方案：删除 `src/custom-tab-bar/`、`pages.json` 的 `tabBar.custom: true`、`utils/custom-tab-bar.ts` 及三个 tab 页的 `syncCustomTabBar` 调用；验证：`npm run build:mp-weixin` 通过，dist 无 custom-tab-bar 目录，会员/游客 tabBar 三栏显示与改造前一致、页面点击恢复正常
- [x] 3.2 摊主会话隐藏 tabBar：新增会话驱动的 hideTabBar/showTabBar 工具，三个 tab 页与摊主页 `onShow` 按会话类型幂等调用；摊主页头部加「我的」入口（`switchTab` 至 `/pages/user/user`）；验证：开发者工具以游客/会员查看三栏正常可点，登录摊主后 tabBar 消失、经摊主页「我的」入口可进入我的页且无底栏，退出摊主后恢复三栏
  （原 3.3 custom-tab-bar 修复任务已随方案切换废弃删除）

## 4. 前端：摊主库存展示 + 今日战绩页

- [x] 4.1 `pages/vendor/index.vue` 商品行显示「剩余 N」，`stock<=0` 显示「售罄」标识；头部加「今日战绩」入口按钮；验证：构建通过，开发者工具目视商品行剩余数量与售罄标识
- [x] 4.2 `apis/stall.ts` 新增 `getVendorTodayStatsAPI` 与返回类型；新建 `pages/vendor/sales.vue`（统计卡 x2 + 商品明细列表，onShow 加载 + 下拉刷新，401/403 跳回摊主页）；`pages.json` 注册；验证：`npm run tsc` 通过、构建通过
- [x] 4.3 今日战绩页暖色样式与摊主页一致（沿用奶油暖色板）；验证：开发者工具目视

## 5. 集成验证

- [ ] 5.1 端到端走查：顾客下单付款 → 摊主商品剩余减少 → 摊主确认收款后今日战绩总额/明细增加 → 取消一单后战绩不变（口径过滤生效）→ 摊主会话 tabBar 仅「我的」→ 退出恢复三栏；验证：本地起后端 + 开发者工具手工全链路

## Workflow follow-up

- 归档本变更时与 `vendor-stall-miniapp` 的 `vendor-mode` delta 合并（本变更以 ADDED 补充，无冲突需求）。
