# Spec Delta

## Purpose

补充摊位订单明细的关联一致性契约：顾客查看订单时，明细 SHALL 与订单严格对应，任何历史遗留或孤儿明细 SHALL NOT 出现在订单中。本能力（stall-order-lifecycle）的主 spec 尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），以 ADDED 需求形式补充，归档时一并合并。

## ADDED Requirements

### Requirement: 订单明细一致性

订单明细的查询 SHALL 同时按订单主键与订单号（order_sn）双重匹配，SHALL NOT 因订单主键被复用或数据异常而将不属于该订单的明细（历史遗留、孤儿数据）归入订单展示。系统中 SHALL NOT 存在 `order_id` 无对应订单的孤儿订单明细。

#### Scenario: 订单明细只含所购商品

- **WHEN** 顾客查看自己的某个摊位订单
- **THEN** 明细仅包含下单时购买的商品，不含任何未购买的历史商品

#### Scenario: 主键复用不串单

- **WHEN** 订单表被清空重用主键编号后创建新订单，而明细表仍残留指向同号的孤儿明细
- **THEN** 新订单的明细查询 SHALL NOT 返回这些孤儿明细
