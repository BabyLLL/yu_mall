# Design

## Context

微信授权登录有两套入口：页面内 `WechatAuthSheet` 弹层（全局状态 `wechatAuthState` 驱动，多页各自挂载实例）与 `pages/public/login.vue` 登录页（member 模式 onLoad 自动开弹层）。`requestWechatAuthorization` 的兜底逻辑：无组件宿主时 `navigateTo` 登录页。未授权顾客首次下单：确认下单页 → 登录页A → onLoad `setTimeout(0)` 开弹层与 sheet 挂载竞态（mountedHosts=0）→ 兜底再 navigateTo 登录页B（redirect 污染为登录页自身）→ A/B 两页弹层同源同显（"弹两遍"）→ 授权成功执行 B 的 finishLogin → redirectTo 登录页C → C 检测已有登录 navigateBack → 停留在登录页。详见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 授权弹层全局唯一，不叠栈、不重复弹
- 授权成功后回到触发页并保持下单上下文（已选清单在 store 中本就持久化）

**Non-Goals:**

- 不改登录接口、会话结构与 `wechatLogin` 流程本身
- 不重构弹层为全局单例组件（维持现有"每页挂载、全局状态驱动"模式，只修行为）
- 不处理管理员登录模式（mode=admin 不涉此次缺陷）

## Decisions

**D1. `wechat-auth.ts` 新增 `showWechatAuthSheet(action)`：仅置 `pendingAction` 并 `visible=true`，不包含任何导航兜底。**
登录页自动开弹层改用它——登录页在 `onReady` 中调用（Vue 子组件 `onMounted` 先于父页 `onReady` 执行，此时该页 sheet 必已 `registerWechatAuthHost`，mountedHosts ≥ 1），从根本上消除竞态，无需重试逻辑。备选（保留 setTimeout 并重试等待挂载）被否：定时器竞态正是本次缺陷形态，不再引入。

**D2. `requestWechatAuthorization` 增加登录页守卫。**
计算当前页 route，若已在 `pages/public/login` 则不再 `navigateTo`（直接返回，由登录页自身负责开弹层），防止叠栈。守卫放在 mountedHosts 判断之前。

**D3. `finishLogin` 回跳目标过滤登录页。**
redirect 解析后若为 `/pages/public/login` 则视为无效，走无 redirect 分支（栈深 >1 则 navigateBack，否则 switchTab 首页）。防御性兜底，正常路径经 D1/D2 后 redirect 不会再是登录页。

**D4. 弹层"弹两遍"不单独处理。**
多页挂载的 sheet 实例共享全局 `wechatAuthState`，叠栈消除（D2）后同屏只会有顶层页一个实例可见，无需改为单例挂载。

## Risks / Trade-offs

- [onReady 时机依赖 Vue 生命周期顺序（子组件 mounted 先于父页 onReady）] → uni-app Vue3 标准生命周期顺序，微信端稳定；若个别端序异常，D2 的守卫仍阻止叠栈，最坏表现为弹层未自动弹出、顾客点「继续」手动打开（页面有按钮入口），有兜底路径。
- [其他调用 requestWechatAuthorization 的页面（我的页、订单页、profile 页）行为回归] → 守卫只影响"当前已在登录页"场景，其他页面路径不变；真机回归覆盖下单、我的页入口两条链路（见任务）。

## Migration Plan

纯前端修复，随小程序发版即生效；无回滚风险（回退即恢复原缺陷行为）。

## Open Questions

（无）
