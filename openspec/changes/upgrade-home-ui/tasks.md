# Tasks

## 1. 后端：菜单接口返回真实剩余库存且不过滤售罄

- [x] 1.1 改造 `StallMenuServiceImpl.listMenu()`：移除售罄过滤，改为计算真实剩余库存（有 SKU 取 `sku.stock - lockStock`，无 SKU 取 `product.stock`）并覆写返回对象的 `stock` 字段；验证：`mvn -pl mall-portal test -Dtest=StallMenuService*`（或新增/更新对应单测）覆盖「售罄商品仍返回且 stock=0」「锁定库存被扣除」两个用例
- [x] 1.2 更新/新增 `StallMenuServiceImpl` 单元测试：有 SKU 扣 lockStock、无 SKU 用商品库存、售罄商品不过滤三个断言；验证：单测全绿

## 2. 前端：首页奶油暖色视觉 + 移除品牌字样

- [x] 2.1 重构 `pages/index/index.vue` 样式变量与配色：按 design D3 色板替换 `$stall-*` 变量，头部文字区（stall-header 整块）删除；验证：首页无「老王烤肠」文字、无深红渐变头部，视觉为奶油暖底 + 橙棕点缀
- [x] 2.2 更新 `pages.json`：首页与全局 `navigationBarTitleText` 改「今日菜单」，首页导航栏背景改杏色系（D3），`backgroundColor` 改 `#FFF9F0`，tabBar `color`/`selectedColor` 同步暖色板；验证：编译后导航栏标题为「今日菜单」、tabBar 选中色为暖橙棕
- [x] 2.3 商品卡片接入剩余数量：卡片信息区显示「仅剩 N 份」（N 为接口返回的真实剩余），`stock <= 0` 时不渲染加单按钮并显示「已售罄」遮罩，`handleAdd` 保留售罄 toast 兜底；验证：分别构造有库存/低库存/零库存商品，卡片展示正确、售罄卡片无 + 按钮
- [ ] 2.4 检查首页其余元素（底部已选条、已选弹层、空态文案）在新色板下的对比度与协调性，必要时微调色值；验证：小程序开发者工具中目视检查各状态渲染正常

## 3. 前端：首页待取货提醒条

- [x] 3.1 在 `pages/index/index.vue` 增加待取货提醒：`onShow` 时若存在有效登录会话则并行调用 `getStallOrderListAPI({ status: 3, pageNum: 1, pageSize: 1 })`，用 `total` 与 `list[0].pickupCode` 渲染提醒条（多条显示「共 N 单，最新取货码 X」），未登录/无单/查询失败均不展示；验证：登录 + 存在待取货订单时首页出现提醒条，未登录时不发请求、无报错
- [x] 3.2 提醒条点击 `uni.switchTab` 跳转「我的订单」，样式按 D3 暖色系（杏色底 + 橙棕强调）；验证：点击提醒条正确切换到订单 tab
- [x] 3.3 待取货提醒定时轮询：`onShow` 立即查一次并启动 30s 定时轮询，`onHide`/`onUnload` 清除定时器，仅登录态启动轮询，轮询失败保留上次结果；验证：开发者工具停留首页，摊主出餐后小票 30s 内自动出现，取货完成后自动消失，切走页面后无持续请求

## 4. 集成验证

- [ ] 4.1 端到端走查：摊主侧改库存/出餐后，顾客首页刷新可见剩余数量变化与待取货提醒出现/消失；验证：本地起后端 + 小程序，手工走查「下单→摊主出餐→首页出现提醒→取货完成提醒消失」全链路

## Workflow follow-up

- 归档本变更时，与 `vendor-stall-miniapp` 的 `stall-menu-ordering` delta 合并（本变更收窄其「今日菜单展示」中"未售罄才展示"的行为）。
