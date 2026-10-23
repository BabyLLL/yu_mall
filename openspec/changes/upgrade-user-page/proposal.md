# Proposal

## Why

「我的」页与设置页仍是旧电商模板的粉色调与占位功能（实名认证/消息推送/清除缓存等点击只弹 toast），与摊位小程序已升级的奶油暖色视觉割裂；且顾客没有任何入口修改自己的昵称——个人资料项是假跳转。

## What Changes

- **全站主题色统一**：`uni.scss` 全局主色变量（`$uni-color-primary`、`$base-color`）由粉色 `#fa436a` 改为橙棕 `#D9622B`，购物车/订单/下单等所有使用全局主色的旧页面随之统一为暖色。
- **「我的」页样式升级**：头部照片背景改为与首页一致的杏色渐变（`#FFE8CC → #FFD9AD`），奶油底 + 白卡片圆角阴影，图标与强调色随新主色；页面结构与交互逻辑（订单入口、摊主模式入口、拖拽回弹）不变。
- **设置页精简**：删除实名认证、消息推送、清除缓存、关于 mall-app-web、检查更新五项占位/残留入口，仅保留「个人资料」「退出登录」。
- **修改个人资料（新增，含后端）**：后端新增 `POST /sso/update` 接口（当前登录会员修改昵称）；前端新建个人资料页，展示头像（本期只读）与昵称编辑，昵称输入框使用微信昵称快速填入能力，保存后本地会话中的会员资料同步更新。

## Capabilities

### New Capabilities

- `member-profile`: 顾客个人资料的查看与修改——会员查看自己的头像与昵称、修改昵称（后端更新接口 + 本地会话同步），以及设置页仅保留个人资料与退出登录的行为边界。

### Modified Capabilities

（无——主题色统一为纯视觉调整，无行为契约变化，落在任务清单中）

## Impact

- **后端**：`mall/mall-portal` — `UmsMemberController` 新增 `POST /sso/update`；`UmsMemberService`/`Impl` 新增 `updateInfo`（复用 `updateByPrimaryKeySelective` 模式）。
- **前端**：`mall-app-web/src` — `uni.scss`（全局主色变量）、`pages/user/user.vue`（样式重构）、`pages/set/set.vue`（删除五项入口）、新建 `pages/user/profile.vue`（个人资料）、`pages.json`（注册新页面）、`apis/member.ts`（新增 updateMemberInfoAPI）、`stores/member.ts`（新增更新本地资料方法）、`types/member.d.ts`（入参类型）。
- **兼容性**：`/sso/update` 为纯新增接口；设置页删除项均为无后端依赖的占位 UI，无数据迁移。
