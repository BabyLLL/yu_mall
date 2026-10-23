# Tasks

## 1. 后端修复

- [x] 1.1 修正 `StallOrderController.list` 对 `stallOrderService.list` 的传参顺序为 `(status, pageNum, pageSize)`；全局核对 mall-portal 中其余 controller→service 的 pageNum/pageSize 传参无同类错位。验证：`mvn compile -pl mall-portal -am` 通过
- [x] 1.2 `StallOrderServiceImpl.list` 与 `StallVendorServiceImpl.listOrders` 空结果分支补 `setList(空列表)`，保证响应恒含 list 字段。验证：curl 无订单会员调 `/stall/order/list` 返回 `"list":[]`

## 2. 回归测试

- [x] 2.1 在 `StallOrderServiceImplTest`（或新增 controller 测试）增加参数顺序回归用例：pageNum=1、pageSize=10 请求时断言 PageHelper 收到 startPage(1, 10)，且空页响应 list 为空数组。验证：`mvn test -pl mall-portal -Dtest=StallOrderServiceImplTest` 通过

## 3. 前端降级与联调

- [x] 3.1 `pages/stall/order-success.vue`：查询失败或首页未匹配到订单时，取货码区显示"获取失败，点击重试"（点击重新 loadOrder），金额显示"—"，不再显示 ¥0.00。验证：`npm run tsc` 通过；断开后端后打开该页可见降级态
- [ ] 3.2 端到端复验原报障路径：登录→下单→下单成功页显示真实取货码与应付金额→"查看订单"列表出现该订单（已下单 tab 与全部 tab 均有数据）。验证：微信开发者工具 + 本地后端走通全程（接口层已用 curl 复验：建单后首页列表含该订单、取货码/金额正确；开发者工具 UI 走查待用户执行）
