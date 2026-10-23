# Proposal

## Why

数据库中存在历史演示数据的孤儿订单明细（`oms_order_item` 中 `order_id` 指向已被清理的旧订单），而摊位订单自增 ID 从头编号后与孤儿数据的 `order_id` 撞号——顾客查看订单时，自己只买的烤肠订单里混入了华为/小米等演示商品的明细（实锤案例：订单 N020/id=16 挂着 5 条孤儿手机明细），严重误导顾客且金额明细失真。

## What Changes

- **数据清理**：一次性删除 `oms_order_item` 中 `order_id` 不存在于 `oms_order` 的孤儿明细行。
- **查询防御**：摊位订单明细查询改为 `order_id + order_sn` 双重匹配（订单号唯一、孤儿行为旧单号，双重条件后不可能再误配），涉及顾客订单列表 `StallOrderServiceImpl.list()`；杜绝同类数据再次串单。
- 前端无需改动（数据正确后展示自然正确）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

> 说明：`stall-order-lifecycle` 的主 spec 尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），沿用项目现行约定以 ADDED 需求补充，归档时一并合并。

- `stall-order-lifecycle`: 新增「订单明细一致性」需求——订单明细 SHALL 与订单唯一对应（order_id + order_sn 双重匹配），SHALL NOT 返回不属于该订单的历史或孤儿明细。

## Impact

- **后端**：`mall/mall-portal` — `StallOrderServiceImpl.list()` 明细查询加 `order_sn` 匹配条件；新增一次性数据清理 SQL（执行于本地/部署库）。
- **数据库**：`oms_order_item` 删除孤儿行（仅删除 `order_id` 无对应订单的行，不影响任何有效数据）。
- **前端**：无改动。
