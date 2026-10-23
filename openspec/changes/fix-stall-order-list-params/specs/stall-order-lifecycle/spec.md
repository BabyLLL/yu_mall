# Spec Delta

## Purpose

补充摊位订单列表查询的分页契约：本能力的主 spec 尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），本变更以 ADDED 需求补充列表查询的行为约束，归档时与主 delta 一并合并。

## ADDED Requirements

### Requirement: 摊位订单列表查询

系统 SHALL 提供当前会员的摊位订单分页查询（含取货码与订单商品明细），分页参数语义 SHALL 与接口声明一致：pageNum 为页码（从 1 起）、pageSize 为每页条数，二者 SHALL NOT 错位互换。列表响应 SHALL 始终包含 list 字段：无订单或查询页超出范围时为空数组而非缺失字段。

#### Scenario: 第一页返回最新订单

- **WHEN** 会员已有若干摊位订单，调用列表查询 pageNum=1、pageSize=10
- **THEN** 返回按创建时间倒序的最新订单，pageNum=1、pageSize=10，list 包含至多 10 条记录，每条含 pickupCode 与订单商品明细

#### Scenario: 空结果返回空数组

- **WHEN** 会员没有任何摊位订单，或请求的页码超出总页数
- **THEN** 响应 code=200，list 为空数组，total 为真实总数

#### Scenario: 下单成功页凭订单ID回查

- **WHEN** 顾客下单成功后进入下单成功页，页面以 pageNum=1 查询列表并按订单 ID 匹配
- **THEN** 刚创建的订单出现在第一页，页面正确展示取货码与应付金额
