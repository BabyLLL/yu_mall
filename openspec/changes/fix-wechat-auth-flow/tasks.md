# Tasks

## 1. 修复授权流程

- [x] 1.1 `utils/wechat-auth.ts`：新增 `showWechatAuthSheet(action)`（置 pendingAction + visible，无导航兜底）；`requestWechatAuthorization` 在 mountedHosts 判断前增加"当前页已是 `pages/public/login` 则直接返回"守卫；验证：`npm run tsc` 通过
- [x] 1.2 `pages/public/login.vue`：member 模式自动开弹层从 `onLoad setTimeout(openWechatSheet, 0)` 改为 `onReady` 调用 `showWechatAuthSheet(finishLogin)`；`finishLogin` 对 redirect 等于登录页的情况过滤（走 navigateBack/首页分支）；验证：`npm run tsc` + `npm run build:mp-weixin` 通过
- [x] 1.3 清理登录页冗余（openWechatSheet 经 requestWechatAuthorization 的旧路径若不再使用则移除，保留「继续」按钮触发开弹层）；验证：构建通过，登录页「继续」可打开弹层

## 2. 集成验证（需开发者工具 + 后端）

- [ ] 2.1 复现场景走查：清缓存模拟首次未授权 → 首页加购 → 去下单 → 提交订单 → 仅弹一个授权层 → 授权成功 → 回到确认下单页且已选清单还在 → 再次提交可下单成功；验证：开发者工具手工走查全链路
- [ ] 2.2 回归走查：我的页设置入口（未登录触发授权）、订单页入口、profile 页入口三条授权链路不叠栈、登录后回跳正确；验证：开发者工具手工走查

## Workflow follow-up

- 归档时新建主 spec `member-auth`（本变更为该能力首个 delta）。
