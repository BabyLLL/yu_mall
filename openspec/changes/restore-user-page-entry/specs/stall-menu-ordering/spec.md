# Spec Delta

## Purpose

补充"我的"页面入口的可达性契约：本能力（stall-menu-ordering）尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），故以 ADDED 需求形式补充，归档时与主 delta 一并合并。

## ADDED Requirements

### Requirement: 我的页面入口

系统 SHALL 提供"我的"页面（账号状态、摊主模式入口、设置、通知），SHALL 将其注册为 tabBar 第三项（今日菜单 + 我的订单 + 我的）：顾客与摊主 SHALL 能通过底部 tabBar 的"我的"标签进入该页面。（2026-10-22 修订：原"SHALL NOT 恢复为独立 tabBar"的约束由用户决定解除，入口方案从订单页导航栏改为 tabBar 第三项。）

#### Scenario: 从 tabBar 进入我的页面

- **WHEN** 顾客或摊主点击底部 tabBar 的"我的"标签
- **THEN** 切换进入"我的"页面，页面正常展示用户信息与功能入口

#### Scenario: 未登录状态进入我的页面

- **WHEN** 未登录用户进入"我的"页面
- **THEN** 页面正常打开并展示游客状态，功能入口按登录态显隐，不报错不白屏
