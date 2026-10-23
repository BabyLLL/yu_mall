# Proposal

## Why

摊位改造（vendor-stall-miniapp）把 tabBar 收敛为 2 个（今日菜单、我的订单）时，`pages/user/user.vue`（"我的"页）从 pages.json 的注册列表中被一并移除，但页面文件与相关逻辑都还在：登录成功后的跳转白名单仍引用 `/pages/user/user`，摊主模式隐藏入口（连点 5 次头像 → admin 登录）、"摊主模式"入口（isAdmin 时显示）、设置入口全部实现在该页面里。页面未注册导致小程序里既打不开也进不去，**摊主因此没有任何入口进入摊主模式**（vendor/index 只能从 user 页到达），设置页、通知页也不可达。用户报障"user 用户页面怎么没有了"。

## What Changes

- **pages.json 重新注册 `pages/user/user`**，恢复"我的"页可达性。
- **新增入口**：「我的」恢复为 tabBar 第三项（今日菜单 + 我的订单 + 我的）。（2026-10-22 修订：原方案为订单页导航栏右侧入口并保留 2-tab 既定设计；实现中发现 mp-weixin 原生导航栏无法承载自定义按钮，经用户决定改为恢复第三个 tab，原约束解除。）
- **回归核对**：登录后跳转、摊主隐藏入口（连点头像 5 次 → admin 登录 → vendor/index）、设置/通知跳转在 user 页恢复后全部重新可达；清理 user.vue 中指向已砍页面的死链接（若有）；user 页转为 tabBar 页后，进入 user 页的跳转方式（switchTab、移除 navigateTo 残留）随同一回归核对。

## Capabilities

### New Capabilities

（无——不引入新能力。）

### Modified Capabilities

- `vendor-mode`：补充"摊主入口可达性"需求——摊主模式入口（"我的"页）SHALL 可从小程序界面到达（订单页导航栏"我的"→ user 页 → 摊主模式入口），否则摊主模式整体不可用。该能力尚未归档（仍在 `vendor-stall-miniapp` 变更 delta 中），本变更以 ADDED 需求补充。
- `stall-menu-ordering`：补充"我的页面入口"需求——顾客 SHALL 能从"我的订单"页导航栏进入"我的"页面（查看账号状态、设置、通知）。

## Impact

- **前端 mall-app-web**：`pages.json`（注册 user 页 + tabBar 第三项含 `static/tab-my` 图标；order 页还原原生导航样式）、`pages/order/order.vue`（移除曾加的自绘导航入口）、`pages/public/login.vue` 跳转白名单（user 页回到 switchTab 名单）、核对 `pages/user/user.vue` 死链接。
- **不动的部分**：后端、tabBar 结构、vendor/index 页面本身、登录逻辑。
