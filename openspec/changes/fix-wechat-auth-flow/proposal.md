# Proposal

## Why

首次使用且未授权的顾客在下单时触发微信授权登录，授权弹层会重复出现（叠栈的两个登录页各渲染一份全局弹层），且授权成功后因 redirect 被污染成登录页自身，顾客被留在登录页、下单流程中断——新顾客的第一单直接流失。

## What Changes

- **修复授权弹层重复弹出**：`requestWechatAuthorization` 增加"当前已在登录页则不再导航"守卫，消除登录页叠栈（弹层重复的根源）。
- **修复登录页自动开弹层的时序竞态**：登录页改为在 `onReady`（子组件已挂载）后通过新增的 `showWechatAuthSheet(action)` 直接打开弹层，不再经由可能触发二次导航的兜底路径。
- **修复 redirect 污染**：登录成功后的回跳目标过滤登录页自身（redirect 为登录页时回退上一页/首页），不再出现"授权完又回到登录页"。

## Capabilities

### New Capabilities

- `member-auth`: 顾客微信授权登录的流程契约——授权弹层同一时刻至多一个、不因重复触发而叠栈，登录成功后回到触发登录的页面并继续被中断的操作（如提交订单）。

### Modified Capabilities

（无）

## Impact

- **前端**：`mall-app-web/src` — `utils/wechat-auth.ts`（新增 `showWechatAuthSheet`、导航守卫）、`pages/public/login.vue`（onReady 开弹层、redirect 过滤）。无后端改动。
- **兼容性**：纯前端行为修复，接口与会话结构不变；已登录用户路径（"当前已有登录"提示）不受影响。
