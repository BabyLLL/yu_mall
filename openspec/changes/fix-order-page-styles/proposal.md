# Proposal

## Why

对「我的订单」页做样式走查，发现多处客观错乱，直接影响订单页（及所有 tab 页底部导航）的可用观感：tabBar 六枚图标中三枚是损坏的纯色方块（`tab-home` 两态、`tab-my` 默认态，已逐一渲染查看确认），两枚选中态图标仍是原模板粉色（#fa436a），与摊位主题红（#C23A22，tabBar `selectedColor`）冲突；订单卡片的 `b-b`/`b-t` 伪元素边框因父级（`.i-top`/`.action-box`）缺少 `position: relative` 而相对页面根定位，卡片内分隔线缺失、可能出现游离细线；空订单状态的 `<empty>` 组件未被 easycom 注册（`components/empty.vue` 平铺路径不匹配默认扫描规则，dist 的 usingComponents 可证），无订单时只剩 uni-load-more 的加载文案。

## What Changes

- **修复 tabBar 图标资产**：重新生成损坏的 `tab-home.png` / `tab-home-current.png` / `tab-my.png`，并将形状完好但颜色为模板粉的 `tab-cate-current.png` / `tab-my-current.png` 重上色为摊位主题红；默认态统一灰色系，与 tabBar `color` 一致。
- **修复订单卡片边框定位**：`order.vue` 的 `.i-top` / `.action-box` 补 `position: relative`，使 `b-b`/`b-t` 伪元素边框回到卡片内部。
- **修复空订单状态**：`order.vue` 显式 import `empty` 组件（沿用 user.vue 显式 import mix-list-cell 的既有模式）；`empty.vue` 由全屏 fixed 布局改为适配列表内嵌的居中布局，并补充空状态文案（目前仅 order.vue 一处活跃使用）。
- **补按压反馈**：`App.vue` 补全局 `.common-hover` 类定义（order.vue 与 user.vue 均在引用该类但从未定义，目前按压无任何反馈）。
- **相邻发现（本次不改）**：`uni.scss` 全局主题变量仍为模板粉（`$base-color`、`$uni-color-primary: #fa436a`），属全站主题统一问题，另行变更处理。

## Capabilities

### New Capabilities

（无。）

### Modified Capabilities

- `stall-menu-ordering`：补充「订单页视觉呈现」需求——tabBar 图标可辨识且两态配色符合主题、订单卡片分隔边框正确、空订单状态有可理解提示、可点击元素有按压反馈。（该能力尚未归档，仍在 `vendor-stall-miniapp` 变更 delta 中，本变更以 ADDED 需求补充，归档时一并合并。）

## Impact

- **前端 mall-app-web**：`static/tab-*.png`（5 枚资产：3 枚重绘、2 枚重上色）、`pages/order/order.vue`（边框定位、空状态、import）、`components/empty.vue`（内嵌布局 + 文案）、`App.vue`（`.common-hover` 定义）。
- **不动的部分**：后端、tabBar 页面组成、页面结构与业务逻辑、vendor 页样式、uni.scss 全局主题变量（另行变更）。
