# Spec Delta

## Purpose

补充摊位菜单页的剩余库存与售罄展示契约，以及首页「奶油暖色 + 去品牌字样」的呈现要求。本能力（stall-menu-ordering）的主 spec 尚未归档（仍在 `vendor-stall-miniapp` 变更的 delta 中），故以 ADDED 需求形式补充：其中「菜单剩余库存与售罄展示」收窄 `vendor-stall-miniapp` delta 中「今日菜单展示」需求"未售罄才展示"的行为（改为含售罄商品一并展示），归档时需一并合并。

## ADDED Requirements

### Requirement: 菜单剩余库存与售罄展示

摊位菜单接口 SHALL 返回所有上架且未删除的商品（含已售罄商品），并为每个商品提供真实剩余库存（有 SKU 时为 `sku.stock - lockStock`，无 SKU 时为 `product.stock`），SHALL NOT 在服务端过滤售罄商品。菜单页 SHALL 在商品卡片上展示剩余数量；剩余为 0 的商品 SHALL 以「已售罄」样式展示且 SHALL NOT 可加单。

#### Scenario: 查看商品剩余数量

- **WHEN** 顾客打开菜单页，某商品剩余库存为 N（N > 0）
- **THEN** 该商品卡片展示剩余数量信息（如「仅剩 N 份」）

#### Scenario: 查看售罄商品

- **WHEN** 某商品真实剩余库存为 0
- **THEN** 该商品仍出现在菜单中，卡片以明确的售罄样式（图片遮罩「已售罄」）展示，且不展示可用的加单按钮

#### Scenario: 对售罄商品加单

- **WHEN** 顾客对剩余为 0 的商品尝试加单
- **THEN** 操作被拒绝并提示该商品已售罄

#### Scenario: 下单扣减后剩余数量更新

- **WHEN** 某商品因下单被锁定库存后顾客刷新菜单
- **THEN** 该商品卡片展示的剩余数量为扣除锁定库存后的真实剩余值

### Requirement: 首页品牌字样移除与奶油暖色视觉

菜单页（首页）SHALL NOT 展示「老王烤肠」品牌字样：页面头部 SHALL NOT 包含品牌名文字，首页导航栏标题与全局导航栏标题 SHALL 为「今日菜单」。首页 SHALL 采用奶油暖色系视觉（杏色/奶咖/奶油黄为主色调，红棕色仅作为按钮等强调点缀），导航栏与 tabBar 配色 SHALL 与之一致。

#### Scenario: 打开首页查看头部

- **WHEN** 顾客打开小程序首页
- **THEN** 页面无「老王烤肠」文字，导航栏标题显示「今日菜单」，页面整体为奶油暖色调

#### Scenario: 查看底部导航配色

- **WHEN** 顾客查看底部 tabBar
- **THEN** tabBar 配色与首页奶油暖色主题一致，选中态使用主题强调色
