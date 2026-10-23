# Tasks

## 1. 后端：昵称修改接口

- [x] 1.1 `UmsMemberService`/`Impl` 新增 `updateInfo`：取当前会员，trim 校验昵称（非空、≤64），`updateByPrimaryKeySelective` 仅更新 nickname，返回更新后的会员信息；验证：新增 `UmsMemberServiceImplTest`（或扩展现有）用例覆盖「正常修改」「空昵称拒绝」「超长拒绝」
- [x] 1.2 `UmsMemberController` 新增 `POST /sso/update`，入参 nickname，返回与 `/sso/info` 同构的会员信息；验证：`mvn -pl mall-portal test -DskipTests=false -Dtest=UmsMember*` 全绿，本地起服务 curl 验证 401（无 token）与正常修改

## 2. 前端：全站主题色统一

- [x] 2.1 `uni.scss` 全局主色 `$uni-color-primary`、`$base-color` 改 `#D9622B`；全局 grep 硬编码 `#fa436a`/`#c23a22` 等旧粉色残留，属于主色语义的一并替换；验证：`npm run build:mp-weixin` 构建通过，产物中无 `fa436a`
- [x] 2.2 「我的」页样式升级：头部照片背景换杏色渐变（#FFE8CC→#FFD9AD），导航按钮改深奶咖，卡片白底圆角阴影与首页一致，订单/列表图标色随新主色；验证：构建后在开发者工具目视检查「我的」页各状态（未登录/会员/管理员会话）
- [ ] 2.3 走查主要旧页面（购物车、订单、确认下单、登录）在暖色主题下的观感，修正突兀配色；验证：开发者工具目视检查无粉色残留、整体色调统一

## 3. 前端：设置页精简 + 个人资料

- [x] 3.1 `pages/set/set.vue` 删除实名认证、消息推送、清除缓存、关于、检查更新五项及对应死代码（switchChange/navToOuter 等），仅留个人资料与退出登录，管理员会话下隐藏个人资料入口；验证：构建后设置页仅两项，管理员会话仅退出登录
- [x] 3.2 新建 `pages/user/profile.vue` 个人资料页：头像只读展示 + `input type="nickname"` 编辑昵称 + 保存按钮，空/超长昵称前端拦截提示；`pages.json` 注册该页；验证：构建通过，从未登录点击被引导登录
- [x] 3.3 `apis/member.ts` 新增 `updateMemberInfoAPI`，`types/member.d.ts` 补入参类型，`stores/member.ts` 新增 `updateMemberProfile`（成功后同步 `authSession.profile` 并持久化）；验证：`npm run tsc` 通过
- [x] 3.4 资料页保存流程接通：调用 store 方法，成功 toast 后返回；「我的」页昵称即时更新；验证：`npm run tsc` + 构建通过，端到端见 4.1

## 4. 集成验证

- [ ] 4.1 端到端走查：登录 → 修改昵称保存 → 「我的」页与个人资料页展示新昵称 → 退出重进登录后昵称仍为新值（后端持久化生效）；验证：本地起后端 + 开发者工具手工走查

## Workflow follow-up

- 归档本变更时新建主 spec `member-profile`（本变更为该能力的首个 delta）。
