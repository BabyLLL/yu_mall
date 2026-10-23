# Design

## Context

摊位改造把 tabBar 收敛为 2 个时，`pages/user/user.vue` 从 pages.json 的 pages 列表中被误删（页面文件保留）。页面承载：登录跳转白名单目标（`login.vue` 的 tabPages 含 `/pages/user/user`）、摊主隐藏入口（连点头像 5 次 → `/pages/public/login?mode=admin`）、"摊主模式"入口（`memberStore.isAdmin` 时显示，跳 `/pages/vendor/index`）、设置/通知入口。页面未注册使这些链路全部断裂，摊主无法进入摊主模式。

## Goals / Non-Goals

**Goals:**
- user 页恢复注册、恢复可达：恢复为 tabBar 第三项（今日菜单 + 我的订单 + 我的）。
- 摊主链路（隐藏登录 → 摊主模式入口 → vendor 页）与顾客链路（设置、通知）重新可用。
- 清理 user 页内指向已砍页面的死链接。

**Non-Goals:**
- 不在订单页保留自绘导航入口（tabBar 方案取代之，自绘代码一并移除）。
- 不改 vendor/index 页、后端。（登录页仅随 user 页成为 tabBar 调整跳转分支。）

## Decisions

1. **入口恢复为 tabBar 第三项**（今日菜单 + 我的订单 + 我的；图标复用现成的 `static/tab-my.png` / `tab-my-current.png`）。方案演变：原定"订单页导航栏右侧入口"并保留 2-tab 既定设计；实现中发现 mp-weixin 原生导航栏不支持自定义按钮（`app-plus.titleNView.buttons` 仅 App/H5 渲染，`onNavigationBarButtonTap` 仅 App/H5，微信导航栏右侧被系统胶囊占据），曾改为自绘导航行过渡；2026-10-22 用户决定解除"不恢复第三个 tab"约束，定为 tabBar 方案——入口一级直达，且天然绕开上述平台限制。订单页还原原生导航样式，自绘导航代码移除。
2. **user 页保持原样注册**，不大改内容：它已按摊位状态机改过（全部订单/已下单入口、摊主模式入口、设置），只需删除或验证指向已砍页面的残留项（足迹/收藏/关注等在改造中已移除入口，本次核对无死链即可）。
3. **登录跳转白名单随 tabBar 方案再次反转**：user 页成为 tabBar 页后，`redirectTo` 到 tab 页会失败，登录页 `finishLogin` 的 tabPages 需重新纳入 `/pages/user/user`（switchTab）。（该条已两次反转：2-tab 期间移出，tabBar 方案后移回。）

## Risks / Trade-offs

- [tabBar 第三项需要图标资源] → 复用模板现成的 `static/tab-my.png` / `tab-my-current.png`，无需新增资源。原"mp-weixin 原生导航栏无法承载自定义按钮"的平台限制随 tabBar 方案天然规避。
- [user 页存在未发现的死链] → 任务中逐一核对其 navigateTo/switchTab 目标均在 pages.json 注册列表内；user 页转 tab 后新增核对：无 navigateTo 指向 user 页的残留调用。

## Migration Plan

纯前端改动，重新编译即生效。回滚 = 还原 pages.json（tabBar 与 order 页样式）、order.vue、login.vue 改动。

## Open Questions

（无。）
