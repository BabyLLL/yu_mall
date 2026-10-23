# Design

## Context

「我的」页（`pages/user/user.vue`）为旧电商模板结构：照片背景头部（`/static/user-bg.jpg`）+ 粉色图标（`#fa436a`）+ 拖拽回弹遮罩；设置页（`pages/set/set.vue`）沿用全局 `uni.scss` 变量，含五个占位入口。全局主色 `uni.scss $uni-color-primary / $base-color: #fa436a`，被购物车/订单/确认下单等旧页面引用。后端 `UmsMemberController(/sso)` 无资料修改接口，`UmsMemberServiceImpl` 已有 `updateByPrimaryKeySelective` 更新模式（`updateIntegration` 同款）。会员资料在本地会话 `AuthSession.profile` 中持久化（`stores/member.ts persistSession`）。首页主题已升级为奶油暖色 + 橙棕 `#D9622B`（`upgrade-home-ui`）。详见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 全局主色变量切到 `#D9622B`，旧页面粉色一处全部统一
- 「我的」页换暖色皮，结构与交互逻辑不动
- 设置页只留「个人资料」「退出登录」
- 昵称可改：后端 `POST /sso/update` + 前端资料页 + 本地会话同步

**Non-Goals:**

- 不做头像修改（无文件上传/存储基础设施，`chooseAvatar` 临时路径不持久——后续引入存储方案再补）
- 不改「我的」页的订单入口、摊主模式入口、头像五连点管理员入口、拖拽回弹等逻辑
- 不动购物车/订单/确认下单等旧页面的布局结构（仅随全局变量变色）
- 不做手机号/性别/生日等其余资料字段

## Decisions

**D1. 主题色统一落在 `uni.scss` 全局变量，不动各页面局部色值。**
`$uni-color-primary` 与 `$base-color` 改为 `#D9622B`，`upgrade-home-ui` 的首页橙棕与之相同色值，全站主色归一。备选（每页单独改）被否：数十处引用，改不完也守不住「统一」。风险：个别旧页面粉色可能以硬编码 `#fa436a` 出现（非变量引用），实现时全局 grep 一并替换，验证方式见任务。

**D2. 「我的」页头部用杏色渐变替换照片背景。**
`user-section` 的 `image.bg` 移除，改为 `linear-gradient(160deg, #FFE8CC, #FFD9AD)`，导航按钮色改深奶咖（`#4A3728`），卡片白底圆角阴影与首页一致。`/static/user-bg.jpg` 不再被引用后不删除（避免影响未知的其他引用，留待后续清理）。拖拽回弹遮罩逻辑保留原样。

**D3. 后端新增 `POST /sso/update`，入参仅 nickname，服务端 trim + 长度校验。**
`UmsMemberController` 新增接口，`UmsMemberService.updateInfo(Long memberId, String nickname)`：trim 后非空、长度 ≤ 64（`ums_member.nickname` 为 varchar(64)），复用 `updateByPrimaryKeySelective` 只更新 nickname 字段。当前会员从 `memberService.getCurrentMember()` 取，不信任前端传 id。返回更新后的 `MemberInfo`（与 `/sso/info` 同构），前端可直接用于会话同步。不修改 phone/sex 等其他字段——入参只有 nickname，天然防越权改字段。

**D4. 前端资料页同步本地会话走 store 新方法，不绕过 persistSession。**
`stores/member.ts` 新增 `updateMemberProfile(nickname)`：调 `updateMemberInfoAPI` 成功后，用返回的 MemberInfo 替换 `authSession.profile` 并 `writeAuthSession` 持久化——复用现有会话结构，「我的」页 `computed memberInfo` 自动刷新，无需额外事件。管理员会话（authType=admin）下不展示个人资料入口（管理员无 UmsMember 记录）——设置页对管理员会话仅展示退出登录。

**D5. 资料页昵称输入用小程序 `input type="nickname"`。**
原生支持微信昵称快速填入（键盘上方出现微信昵称建议），无需额外授权。头像展示用 `memberInfo.icon`，只读无上传入口。

**D6. 设置页删除五项后，「个人资料」从假 toast 改为真实跳转。**
`navTo` 的占位分支在删除后自然只剩真实 URL 跳转，简化为直接 `uni.navigateTo`。退出登录逻辑不动。

## Risks / Trade-offs

- [全局变量变色影响未逐一走查的旧页面] → 变量替换是机械变更，构建后对主要页面（购物车/订单/下单/首页）开发者工具走查一遍；发现突兀的硬编码粉色再单独处理（验证方式见任务）。
- [`/sso/update` 被并发会话调用] → 接口语义为"改当前登录会员自己的昵称"，与会话绑定，无并发冲突面；最坏情况是后写覆盖先写（用户自己两个设备），可接受。
- [管理员会话误入资料页] → 设置页按会话类型隐藏入口 + 页面内兜底跳回，双保险（见 D4）。
- [微信昵称快速填入在非微信端（H5/APP）不可用] → `type="nickname"` 在不支持的平台退化为普通输入框，手动输入始终可用。

## Migration Plan

后端先行（纯新增接口），前端随后发布。回滚：前端回退即可，后端接口无副作用可独立保留。

## Open Questions

（无）
