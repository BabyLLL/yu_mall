# Design

## Context

本地库实锤：`oms_order_item` 中存在种子数据的孤儿明细（id 41-45，华为 P20/小米 8/红米 5A/iPhone 8 Plus，`order_id`=16），而 `oms_order` 已被清理重建、自增 ID 从 1 重新编号；今日摊位订单 N020 恰好拿到 id=16，`StallOrderServiceImpl.list()` 按 `andOrderIdIn` 查明细时把孤儿行一并捞出。写入侧（`create()` + `PortalOrderItemDao.insertList`）无缺陷。详见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 清掉孤儿明细数据
- 明细查询对脏数据免疫（双重匹配）

**Non-Goals:**

- 不追究历史数据如何产生（演示库清理方式不明，不再复现即可）
- 不改下单写入链路（无缺陷）
- 不做数据库层外键约束（避免影响既有运维方式）

## Decisions

**D1. 查询防御用 `order_id + order_sn` 双重匹配。**
`StallOrderServiceImpl.list()` 查明细时在 `andOrderIdIn(orderIds)` 基础上追加 `andOrderSnIn(orderSns)`：id 与 sn 是一对一关系，新订单不可能与孤儿行同时匹配两个条件（孤儿行的 sn 是旧格式 `2018xxxx`），且不增加查询次数。备选（逐单循环按 sn 查）被否：N 次查询；备选（只清理数据不加防御）被否：无法阻止同类脏数据再次串单。

**D2. 孤儿数据一次性 SQL 清理，不做应用层自动任务。**
清理语句：`DELETE FROM oms_order_item WHERE order_id NOT IN (SELECT id FROM oms_order)`。作为任务在本地/部署库各执行一次并核对删除行数；不引入定时任务（孤儿数据来源是历史演示数据清理事故，非常态）。

**D3. 只改顾客端摊位订单列表查询，不扩散到摊主端。**
摊主端 `StallVendorServiceImpl.listOrders()` 同样存在按 orderId 查明细的代码，同样会受孤儿数据影响——本次一并加 `andOrderSnIn` 防御（同款一行改动），避免修一半。今日战绩统计（`todayStats`）按 `andOrderIdIn` 聚合明细，同样加防御。

## Risks / Trade-offs

- [DELETE 误删有效数据] → 条件为"order_id 无对应订单"，此类明细不可能被任何订单查询命中，删除无业务损失；执行前先 SELECT 核对行数。
- [双重匹配遗漏其他按 orderId 查明细的位置] → 全局 grep `OmsOrderItemExample` 核对所有使用点，逐一加 sn 条件（任务中列出）。

## Migration Plan

后端代码与数据清理无依赖顺序（防御先上更稳），一并发布；回滚无需（查询条件收窄不影响正常数据）。

## Open Questions

（无）
