# Tasks

## 1. 查询防御

- [ ] 1.1 `StallOrderServiceImpl.list()`：明细查询追加 `andOrderSnIn(orderSns)` 双重匹配；更新 `StallOrderServiceImplTest` 增加用例「孤儿明细（同 order_id 旧单号）不出现在订单明细中」；验证：`mvn -pl mall-portal test -DskipTests=false -Dtest=Stall*` 全绿
- [ ] 1.2 `StallVendorServiceImpl` 的 `listOrders()` 与 `todayStats()` 明细查询同款追加 `andOrderSnIn`；验证：单测全绿（今日战绩用例补孤儿不计数断言）
- [ ] 1.3 全局 grep `OmsOrderItemExample` 核对剩余使用点，存在同风险的一并加 sn 条件；验证：grep 结果全部覆盖或注明无需理由

## 2. 数据清理（本地库执行，部署时同步）

- [ ] 2.1 执行清理：先 `SELECT count(*) FROM oms_order_item WHERE order_id NOT IN (SELECT id FROM oms_order)` 核对行数，再执行对应 DELETE，并复查为 0；验证：复查 SQL 返回 0，订单 N020 详情仅剩淀粉肠一条明细

## 3. 集成验证（需开发者工具 + 后端）

- [ ] 3.1 走查：顾客"我的订单"各 tab 订单明细只含所购商品；摊主订单队列与今日战绩无手机等演示商品混入；验证：开发者工具目视 + 接口返回核对

## Workflow follow-up

- 归档时与 `vendor-stall-miniapp` 的 `stall-order-lifecycle` delta 合并（本变更 ADDED 补充，无冲突需求）。
