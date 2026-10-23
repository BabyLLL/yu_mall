# Design

## Context

样式走查的客观证据（静态核查 + 资产渲染查看 + dist 产物核对）：

- `static/` 下 6 枚 tabBar 图标：`tab-home.png`、`tab-home-current.png`、`tab-my.png` 为纯色方块（损坏占位图）；`tab-cate-current.png`、`tab-my-current.png` 形状完好但为模板粉 #fa436a；仅 `tab-cate.png` 完全正常。tabBar 配置 `color: #8A7A6D` / `selectedColor: #C23A22`。
- `order.vue` 的 `.i-top`、`.action-box` 使用全局 `b-b`/`b-t` 类（App.vue 用 `:after` 伪元素 + `position: absolute` 实现 0.5px 细线），但这两个父级未设 `position: relative`，伪元素相对页面根定位。
- `<empty>` 未注册：`components/empty.vue` 为平铺路径，easycom 默认规则只匹配 `components/<name>/<name>.vue`；dist 的 `pages/order/order.json` usingComponents 仅含 uni-load-more。`empty.vue` 自身是全屏 `position: fixed` 布局（#f5f5f5 覆层），也不适合嵌在订单列表 scroll-view 内。
- `common-hover` 被 order.vue 与 user.vue 引用但全工程无定义（无按压反馈）。
- 相邻问题（不改）：`uni.scss` 的 `$base-color`、`$uni-color-primary` 仍为模板粉 #fa436a。

## Goals / Non-Goals

**Goals:**
- 三枚 tabBar 图标语义清晰、两态配色符合主题（灰默认态 + #C23A22 选中态）。
- 订单卡片边框回到卡片内部，无游离细线。
- 空订单状态可见、可理解。
- 按压反馈生效。

**Non-Goals:**
- 不清理 `uni.scss` 模板粉全局变量（影响全站，另行变更）。
- 不改订单页结构/业务逻辑、tabBar 页面组成、vendor 页样式。
- 不处理已砍页面（brandAttention/productCollection/readHistory）中的 empty/common-hover 引用（页面未注册，不可达）。

## Decisions

1. **图标资产用脚本绘制重建**：以现存完好的 `tab-cate.png` 的尺寸与留白风格为基准，绘制菜单（今日菜单）与人形（我的）图形；默认态灰 #8A7A6D 系、选中态 #C23A22；`tab-cate-current.png`、`tab-my-current.png` 形状保留、整体重上色为主题红。备选：从外部图标库取图——否（需联网与许可核查，且风格难与现有一致）；直接用纯 CSS 图标——否（小程序 tabBar 只接受图片路径）。绘制工具在实施时确定（如 Pillow 脚本），验收以 PNG 渲染结果为准。
2. **边框修复用最小改动**：`.i-top`、`.action-box` 补 `position: relative`（保留 App.vue 的 0.5px hairline 方案与现有类名）。备选：改用普通 `border-top/bottom`——会丢 0.5px 细线效果且改动面更大，不取。
3. **空状态就地修复**：`order.vue` 显式 `import empty from '@/components/empty.vue'`（沿用 user.vue 显式 import mix-list-cell 的既有模式，不依赖 easycom）；`empty.vue` 布局从全屏 fixed 改为内嵌居中（组件目前仅 order.vue 一处活跃使用，改动无外溢），并补充空状态文案（如「还没有订单，去今日菜单点一单吧」）。
4. **按压反馈补全局类**：`App.vue` 新增 `.common-hover`（透明度/背景轻变化的按压态），两处既有引用（order.vue、user.vue）即刻生效，无需改调用点。备选：移除 hover-class 引用——会丢交互反馈，不取。

## Risks / Trade-offs

- [脚本绘制图标风格可能与手绘资产有细微差异] → 以 `tab-cate.png` 为基准对齐尺寸/留白/线宽；验收用图片渲染逐一查看。
- [绘制工具在本机不可得（如无 Pillow）] → 实施时先探测可用工具（Python/Pillow、Node canvas），或以最小依赖方式生成；实在不可得则暂停并说明。
- [empty.vue 布局改动影响对 fixed 空态的潜在预期] → 全工程仅 order.vue 活跃使用，已核实无外溢。

## Migration Plan

纯前端改动（资产 + 样式 + 组件布局），重新编译即生效。回滚 = 还原 5 枚 PNG 与 order.vue / empty.vue / App.vue 三处改动。

## Open Questions

（无。）
