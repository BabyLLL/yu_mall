# Proposal

## Why

摊位改造联调时发现两个顾客端问题，都发生在"已下完单"之后：

1. **下单成功页不显示取货码和应付金额**。下单成功页需要凭 orderId 反查订单，但它复用了订单列表接口做单笔查询。而后端 `StallOrderController.list` 与 `StallOrderService.list` 的参数声明顺序不一致：controller 按前端传参把 `(status, pageSize, pageNum)` 原样传给 `list(status, pageSize, pageNum)`，而 service 签名是 `list(Integer status, Integer pageNum, Integer pageSize)`——pageSize 与 pageNum 位置互换。前端传 pageNum=1、pageSize=10 时，实际执行 `pageNum=10、pageSize=1` 的查询，第 10 页通常为空，订单查不到，页面永远显示"取货码 ..."和 ¥0.00。
2. **"我的订单"页无数据**。同一参数错位使列表查询 `pageNum=10、pageSize=1`，前端第一页拿到的是"第10页的第1条"，大多数情况下为空列表；`CommonPage.restPage` 按真实分页返回 pageNum=10，前端也不展示。已验证：数据库中会员 14 存在 4 笔摊位订单（N015–N018），接口却返回 total=4、list 为空。

另有一个同根但独立的表现：列表响应中 `list` 字段缺失（而非空数组），前端 `res.data?.list || []` 可兜底，但订单成功页的 `find` 也因此必失败。

## What Changes

- **后端（BREAKING 行为修复）**：修正 `StallOrderController.list` 到 `StallOrderService.list` 的参数传递顺序（pageSize/pageNum 错位），使分页参数语义与声明一致；`StallVendorController.orders` 与 `StallVendorServiceImpl.listOrders` 参数顺序本已一致，核对后不动。补一个 controller↔service 参数顺序的单测防止回归。
- **后端**：修复分页转装时空结果不返回 `list` 字段的问题——`StallOrderServiceImpl.list` 与 `StallVendorServiceImpl.listOrders` 空页时 `setList(Collections.emptyList())`，保证响应结构稳定。
- **前端**：下单成功页不再依赖列表接口反查单笔订单——后端下单接口返回订单 ID 后，改为直接取列表第一页并在本地按 ID 匹配的现状可保留，但要求列表接口修复后此逻辑自然生效；同时为 `order-success` 增加查询失败/订单不在首页时的降级展示（金额与状态显示"—"，取货码显示获取失败提示并可重试），不再出现误导性的 ¥0.00。
- **前端**：`order-success` 查询接口按新语义传参（现状代码传参在修复后即为正确语义，核对即可），订单列表页无需改动（其 `pageNum=1、pageSize=5` 传参在修复后恢复正确）。

## Capabilities

### New Capabilities

（无——不引入新能力。）

### Modified Capabilities

- `stall-order-lifecycle`：补充"摊位订单列表查询"的需求——分页参数语义 SHALL 正确（pageNum/pageSize 不得错位），列表响应 SHALL 始终包含 list 字段（空页为空数组），这是取货码展示与订单列表的既有场景能成立的前提。该能力尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），本次在该 delta 能力路径上新建修正 delta。

## Impact

- **后端 mall-portal**：`StallOrderController.list`（参数传递）、`StallOrderServiceImpl.list` / `StallVendorServiceImpl.listOrders`（空列表 setList）、`StallOrderServiceImplTest`（新增参数顺序回归测试）。
- **前端 mall-app-web**：`pages/stall/order-success.vue`（失败降级展示与重试；传参核对）。
- **不改**：数据库、API 路径、前端订单列表页逻辑。
