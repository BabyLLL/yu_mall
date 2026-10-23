# Spec Delta

## Purpose

补充摊主模式的运营可见性契约：真实库存可见、今日战绩统计、摊主会话下的 tabBar 呈现。本能力（vendor-mode）的主 spec 尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），故以 ADDED 需求形式补充，归档时与主 delta 一并合并。

## ADDED Requirements

### Requirement: 摊主真实库存可见

摊主商品列表 SHALL 展示每个商品的真实剩余库存（有 SKU 时为 `sku.stock - lockStock`，无 SKU 时为 `product.stock`），SHALL NOT 展示未扣锁定库存的原始商品库存。真实剩余为 0 的商品 SHALL 以售罄标识展示。

#### Scenario: 查看商品剩余

- **WHEN** 摊主打开商品管理列表，某商品被顾客下单锁定了部分库存
- **THEN** 该商品行展示的剩余数量为扣除锁定库存后的真实剩余值

#### Scenario: 售罄标识

- **WHEN** 某商品真实剩余为 0
- **THEN** 该商品行展示售罄标识

### Requirement: 今日战绩统计

系统 SHALL 为摊主提供今日战绩独立页面，仅摊主（管理员）会话可访问。统计 SHALL 采用实收口径：今日创建且状态为已付款/制作中/待取货/已完成（排除未付款与已取消）的摊位订单。页面 SHALL 展示今日总额、订单数与商品明细（商品名 x 数量 x 金额），并支持下拉刷新。

#### Scenario: 查看今日战绩

- **WHEN** 摊主从摊主页进入今日战绩页面
- **THEN** 页面展示今日总额、订单数与按商品聚合的销售明细（名称、数量、金额）

#### Scenario: 实收口径过滤

- **WHEN** 今日存在已取消或未付款（已下单）状态的摊位订单
- **THEN** 这些订单的金额与商品数量 SHALL NOT 计入今日战绩

#### Scenario: 顾客访问被拒

- **WHEN** 非摊主会话调用今日战绩统计接口
- **THEN** 请求被拒绝（401/403），页面不可见数据

### Requirement: 摊主会话 tabBar 定制

摊主（管理员）会话下小程序 SHALL 隐藏底部 tabBar，SHALL NOT 展示「今日菜单」与「我的订单」入口；摊主模式页面 SHALL 提供「我的」页入口以便返回会员视图。会员与游客会话下 tabBar SHALL 保持「今日菜单」「我的订单」「我的」三项显示，行为与静态 tabBar 一致。

#### Scenario: 摊主会话无底部导航

- **WHEN** 摊主以管理员会话停留在任意页面
- **THEN** 底部 tabBar 隐藏，不可见「今日菜单」与「我的订单」入口

#### Scenario: 摊主返回我的页

- **WHEN** 摊主点击摊主模式页的「我的」入口
- **THEN** 跳转「我的」页，tabBar 保持隐藏直至退出摊主模式

#### Scenario: 顾客会话不受影响

- **WHEN** 会员或游客停留在任一 tab 页
- **THEN** 底部 tabBar 显示今日菜单、我的订单、我的三项，点击行为与原有静态 tabBar 一致

#### Scenario: 会话切换后恢复

- **WHEN** 摊主退出摊主模式回到会员/游客会话
- **THEN** tabBar 恢复三栏显示
