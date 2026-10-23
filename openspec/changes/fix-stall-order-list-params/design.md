# Design

## Context

`vendor-stall-miniapp` 变更已实现摊位下单/列表/取消与摊主模式，联调发现顾客端下单后两张页面异常：

- `StallOrderController.list` 把请求参数按 `(status, pageSize, pageNum)` 传给签名为 `list(Integer status, Integer pageNum, Integer pageSize)` 的 service（`StallOrderServiceImpl.java:192`），pageSize/pageNum 错位。前端传 `pageNum=1, pageSize=10` 实际执行 `PageHelper.startPage(10, 1)`——查"第 10 页、每页 1 条"，结果几乎恒为空。已用 curl 复现：member "test" 建单后 list 返回 total=1 但 list 为空；数据库中会员 14 有 4 笔摊位订单（N015–N018）但接口返回空列表。
- `StallOrderServiceImpl.list` 在空页时提前 return，`resultPage.list` 从未 set，响应 JSON 缺失 `list` 字段（96 字节的响应体）。前端用 `res.data?.list || []` 兜底，但 order-success 页的 `orders.find(...)` 因此必然失败。
- `StallVendorServiceImpl.listOrders` 存在同样的空页不 setList 问题；其参数顺序（controller `(status, pageSize, pageNum)` → service `(status, pageNum, pageSize)`，controller 内部已按 service 顺序重排）核对无误，不动。

## Goals / Non-Goals

**Goals:**
- 分页参数语义恢复正确：pageNum=1 返回最新订单，列表页与下单成功页恢复正常数据。
- 列表响应结构稳定：list 恒存在（空页为 `[]`）。
- order-success 页在查不到订单时给出可理解的降级展示与重试，不再显示误导性 ¥0.00。
- 增加 controller↔service 参数顺序的回归测试。

**Non-Goals:**
- 不新增"按订单 ID 查单笔"接口（order-success 用首页列表匹配已足够；加接口属后续优化）。
- 不改 `StallVendorController` 参数顺序（已正确）。
- 不动前端订单列表页、取消订单流程、数据库与 API 路径。

## Decisions

1. **修 controller 传参而非统一签名**：controller 调用处改为 `stallOrderService.list(status, pageNum, pageSize)`。service 签名保持 `(status, pageNum, pageSize)`（与 `StallVendorService.listOrders` 一致，二者对齐）。理由：改动最小、两处签名一致降低再次错位的概率。
2. **空页 setList 空数组**：`StallOrderServiceImpl.list` 与 `StallVendorServiceImpl.listOrders` 在 `CollUtil.isEmpty(orderList)` 分支补 `resultPage.setList(new ArrayList<>())`。理由：响应契约稳定，前端不需要防御字段缺失。
3. **order-success 降级展示**：查询失败或首页 10 条内找不到目标订单时，取货码区显示"取货码获取失败，下拉或点击重试"，金额显示"—"；提供点击重试。理由：列表修复后正常路径必然命中，降级只兜底异常（网络失败、并发下订单被挤出一页），不应显示 ¥0.00 误导顾客。备选方案（加单笔查询接口）被否：为兜底场景扩接口不值得。
4. **回归测试**：在 `StallOrderServiceImplTest` 增加"list 首页参数顺序"用例——mock mapper 验证 `PageHelper.startPage` 收到 (1, 10) 而非 (10, 1)；参数顺序错位在 controller 层，单测以"controller 传参→service mock 断言"覆盖（MockMvc 或直接 new controller 传参断言）。

## Risks / Trade-offs

- [其他 controller/service 存在同类参数错位] → 已核对 `StallVendorController/ServiceImpl` 无错位；本次全局 grep `(pageSize, pageNum)` 传参模式复核一遍，作为 tasks 中的核对项。
- [order-success 首页 10 条内匹配不到（极端并发）] → 降级展示 + 重试按钮兜底；后续若摊位单量大可加单笔接口。
- [前端旧版本缓存仍在传参] → 传参本身没变（一直是 pageNum=1、pageSize=10），修复纯后端，前端无兼容问题。

## Migration Plan

纯代码修复，无数据迁移。后端重启即生效，前端仅 order-success 增加降级逻辑。回滚 = 还原两处后端改动。

## Open Questions

（无。）
