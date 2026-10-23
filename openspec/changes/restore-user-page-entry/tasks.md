# Tasks

## 1. 恢复页面注册与入口

- [x] 1.1 pages.json 重新注册 `pages/user/user`（保持原有页面样式配置），核对注册顺序不影响启动页。验证：`npm run dev:mp-weixin` 构建通过，dist 的 app.json pages 列表含该页
- [x] 1.2 「我的」恢复为 tabBar 第三项：pages.json tabBar 增加 `pages/user/user`（icon 用现成 `static/tab-my.png` / `tab-my-current.png`，文字"我的"），order 页还原原生导航样式（移除 navigationStyle:custom）；order.vue 移除自绘导航行、"我的"入口及关联样式/逻辑（含 swiper 高度还原）。验证：构建通过，tabBar 三项可点，"我的"切到 user 页，订单页导航栏恢复原生标题
- [x] 1.3 修正 `pages/public/login.vue` 的 `finishLogin`：`/pages/user/user` 不在 tabBar，从 switchTab 白名单移到 redirectTo 分支。验证：未登录下单 → 登录成功 → 回到 confirm-order；从 user 页触发登录 → 登录成功 → 落在 user 页不报错（注：1.4 随 tabBar 方案将 user 页移回 switchTab 白名单）

- [x] 1.4 user 页成为 tabBar 页后的跳转回归：`pages/public/login.vue` 的 `finishLogin` tabPages 重新纳入 `/pages/user/user`（switchTab；redirectTo 到 tab 页会失败）；核对全工程无 `uni.navigateTo` 指向 `/pages/user/user` 的残留调用。验证：登录后跳 user 页正常；构建通过（已核对：src 下无 navigateTo 指向 user 页的残留，构建通过，dist app.json tabBar 三项含 user 页）

## 2. 死链核对与端到端

- [x] 2.1 核对 user.vue 所有 navigateTo/switchTab 目标均在 pages.json 注册列表内，删除指向已砍页面的残留入口（如有）。验证：逐条核对清单产出，无死链
- [ ] 2.2 端到端走查：顾客链路（tabBar 点"我的" → 设置/通知、全部订单/已下单）；摊主链路（我的 → 连点头像 5 次 → admin 登录 → 摊主模式入口 → vendor 页操作订单队列）。验证：微信开发者工具全程走通
