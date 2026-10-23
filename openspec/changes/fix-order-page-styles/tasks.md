# Tasks

## 1. tabBar 图标资产修复

- [ ] 1.1 探测可用的图像绘制工具（Python+Pillow 或 Node 方案），确定图标生成脚本方案并落定脚本位置。验证：脚本可在本机跑通
- [ ] 1.2 重新生成损坏的 `tab-home.png` / `tab-home-current.png` / `tab-my.png`，并将 `tab-cate-current.png` / `tab-my-current.png` 重上色为主题红：菜单/订单/我的图形语义清晰，默认态灰色系（对齐 `#8A7A6D`），选中态 `#C23A22`，尺寸与留白对齐现存 `tab-cate.png`。验证：逐一渲染查看 6 枚 PNG，无方块占位图
- [ ] 1.3 重新构建（`npm run build:mp-weixin`），核对 dist 产物引用的图标文件已更新。验证：构建通过，DevTools 中三个 tab 两态显示正确

## 2. 订单页样式修复

- [ ] 2.1 `order.vue` 的 `.i-top` / `.action-box` 补 `position: relative`。验证：构建通过；DevTools 中卡片头部与操作栏分隔线位于卡片边界内，滚动列表无游离细线
- [ ] 2.2 空状态：`order.vue` 显式 `import empty from '@/components/empty.vue'`；`empty.vue` 由全屏 fixed 改为列表内嵌居中布局并补空状态文案。验证：无订单时展示空状态图示与文案，不出现「暂无更多」；构建通过
- [ ] 2.3 `App.vue` 补全局 `.common-hover` 按压态类。验证：按下订单卡片/用户页功能入口有可见按压反馈

## 3. 整页走查

- [ ] 3.1 「我的订单」页整页样式走查：原生导航栏、筛选 tab 行、订单卡片（含取货码大字块、备注块）、操作栏、空态、下拉刷新，对照 spec 三条 ADDED 需求逐项核对。验证：微信开发者工具全程走通，无错乱项残留
