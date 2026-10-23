# Spec Delta

## Purpose

补充摊位小程序「我的订单」页的视觉呈现契约：本能力（stall-menu-ordering）尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），故以 ADDED 需求形式补充，归档时与主 delta 一并合并。

## ADDED Requirements

### Requirement: tabBar 图标呈现

底部 tabBar 三项（今日菜单、我的订单、我的）的图标 SHALL 为可辨识的图形，SHALL NOT 呈现为纯色方块或残缺占位图。图标默认态 SHALL 使用灰色系（与 tabBar `color` 协调），选中态 SHALL 使用摊位主题红（#C23A22），与 tabBar `selectedColor` 及整体主题一致。

#### Scenario: 查看底部导航

- **WHEN** 顾客在任一 tab 页查看底部 tabBar
- **THEN** 三枚图标清晰可辨（菜单、订单、我的语义可对应），未选中项为灰色图标，选中项图标与文字均为主题红

#### Scenario: 切换 tab

- **WHEN** 顾客点击「我的」或「今日菜单」切换 tab
- **THEN** 新选中项图标切换为主题红态，原选中项恢复灰色态，无方块占位图出现

### Requirement: 订单卡片分隔与边框

订单卡片的头部信息行与操作栏 SHALL 展示正确的分隔线：卡片内的细线边框 SHALL 相对卡片自身定位渲染，页面 SHALL NOT 出现游离于卡片之外的细线。

#### Scenario: 查看待支付订单卡片

- **WHEN** 列表渲染带「取消订单 / 查看详情」操作栏的订单卡片
- **THEN** 卡片头部（时间/状态）与商品区之间有分隔线，操作栏顶部有分隔线，分隔线均位于卡片边界内

#### Scenario: 浏览订单列表

- **WHEN** 顾客上下滚动订单列表
- **THEN** 页面除卡片自身边框外无游离的细线或错位线段

### Requirement: 空订单状态提示

无任何订单时，订单页 SHALL 展示可理解的空状态提示（图示与/或文案），SHALL NOT 仅展示分页加载类文案（如「暂无更多」）作为空态。

#### Scenario: 新顾客打开我的订单

- **WHEN** 无任何订单的顾客进入「我的订单」页
- **THEN** 页面展示空状态提示，引导顾客前往「今日菜单」下单，不出现「暂无更多」等分页文案

### Requirement: 列表按压反馈

订单列表中可点击的元素（订单卡片、操作按钮）SHALL 具有可见的按压反馈。

#### Scenario: 按下订单卡片

- **WHEN** 顾客按下某张订单卡片
- **THEN** 卡片出现可见的按压态反馈，松开后恢复
