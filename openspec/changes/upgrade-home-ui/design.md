# Design

## Context

摊位小程序首页（`mall-app-web/src/pages/index/index.vue`）当前为红棕深色系（`$stall-red: #C23A22` 渐变头部 + `$stall-dark: #3D2B1F` 底部条），头部展示「老王烤肠」品牌名。后端菜单接口 `StallMenuServiceImpl.listMenu()` 在服务端过滤真实库存 ≤ 0 的商品，且返回的 `PmsProduct.stock` 是商品表库存，未扣除 SKU 锁定库存。订单状态机已有 `READY=3 待取货`，顾客端已有 `getStallOrderListAPI({status})` 分页查询接口。详见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 首页整体切换为轻快奶油暖色系，红棕仅作点缀
- 移除全部「老王烤肠」字样（页面头部 + pages.json 导航栏标题）
- 菜单接口返回真实剩余库存且不过滤售罄商品；首页卡片展示「仅剩 N 份」/「已售罄」
- 首页待取货提醒条（登录态 + status=3 查询）

**Non-Goals:**

- 不改摊主端（vendor）页面配色与其他页面样式（订单页已在 `fix-order-page-styles` 中处理）
- 不做消息推送、不做「制作中/已付款」的首页提醒
- 不引入新的库存字段语义（如预计补货时间）
- 不改动下单/取消/取货码流程

## Decisions

**D1. 菜单接口在返回前用真实剩余库存覆写 `stock` 字段，不新增 DTO。**
`listMenu()` 遍历商品时计算 `realStock = sku.stock - lockStock`（无 SKU 用 `product.stock`），直接 `product.setStock(realStock)` 后返回。理由：前端类型 `StallMenuItem` 已含 `stock`，零类型改动、零新模型；该接口只服务摊位菜单，「stock=真实可售」的语义在本接口内自洽。备选：新增 `StallMenuVO.realStock` 字段——更"干净"但要动 controller/domain/前端类型三层，收益不成比例。
注意：`selectByExample` 返回的是受管对象但 MyBatis 无一级缓存污染问题（每次新查），直接 set 安全。

**D2. 售罄判断与过滤逻辑替换：`isSoldOut` 过滤改为计算真实库存。**
现有 `isSoldOut()` 每商品一次 SKU 查询，改造后同样每商品查一次 SKU 用于计算剩余量（商品量级为摊位菜单，几十条以内，不引入批量优化；如后续商品数增长可合并为一次 `andProductIdIn` 批量查询——列为实现时的可选优化，不作为任务硬性要求）。

**D3. 奶油暖色板**（在 index.vue 内定义 SCSS 变量，命名沿用现有 `$stall-*` 前缀）：

| 用途 | 变量 | 色值 |
|---|---|---|
| 页面背景 | `$stall-bg` | `#FFF9F0` 奶油白 |
| 卡片 | `$stall-card` | `#FFFFFF` |
| 头部/提示条底 | 杏色渐变 | `#FFE8CC → #FFD9AD` |
| 强调色（按钮/价格/tabBar 选中） | `$stall-accent` | `#D9622B` 暖橙棕 |
| 主文字 | `$text-main` | `#4A3728` 深奶咖 |
| 次文字 | `$text-sub` | `#A08C7A` |
| 底部已选条 | `$stall-bar` | `#4A3728`（深奶咖，替代纯深棕） |

具体色值实现时可微调，原则：大面积低饱和暖底 + 单一橙棕强调色，不再使用大面积深红渐变。`pages.json` 同步：首页 `navigationBarBackgroundColor` 改为杏色（如 `#FFE8CC`）+ 黑字，`backgroundColor` 改 `#FFF9F0`；tabBar `selectedColor` 改 `$stall-accent` 对应色值，`color` 改 `$text-sub` 对应色值。全局导航栏标题改「今日菜单」。

**D4. 头部简化：删除整个 `stall-header` 文字区。**
首页直接以提醒条/菜单区开头；`stall-sub`（现烤现卖 · 到摊自取）一并移除（用户已确认整个头部简化）。顶部留白由导航栏 + 页面 padding 承担。

**D5. 待取货提醒条复用现有接口，不新增后端端点，停留期间 30 秒定时轮询。**
登录判定复用现有会话（http 层同款 storage session 读取），已登录才调用 `getStallOrderListAPI({ status: 3, pageNum: 1, pageSize: 1 })`，用返回的 `total` 与 `list[0].pickupCode` 渲染；多条时展示「共 N 单，最新取货码 X」。轮询决策：

- 间隔 30 秒（摊位场景订单状态变化频率低，30s 足够且不压后端；如需调整改一处常量）
- 生命周期：`onShow` 立即查一次并启动定时器，`onHide`/`onUnload` 清除——仅首页前台展示期间轮询，切走即停
- 未登录不启动轮询；轮询失败静默并保留上次成功结果（已有提醒条不因一次失败闪烁消失）

点击提醒条 `uni.switchTab` 跳「我的订单」。备选（新增 `/stall/order/readySummary` 轻量端点）被否：为一条提示条加后端接口不划算，且分页接口 total 已够用。备选（轮询间隔 10s）被否：更新及时性提升有限，请求量翻三倍。

**D6. 售罄卡片禁用加单：隐藏 `+` 按钮而非置灰。**
售罄卡片不渲染 `add-btn`，点击卡片不触发加单；`handleAdd` 保留 `stock <= 0` 兜底 toast（防御直接调用路径）。已选弹层中的 `+` 也受 `entry.product.stock` 上限约束的现有逻辑不变（后端下单仍有库存校验兜底，前端不额外做超卖拦截——与本能力现有行为一致）。

## Risks / Trade-offs

- [旧版小程序缓存的前端不认识新语义的 stock] → 旧版把售罄商品当普通商品展示，加单时后端 `create()` 仍有 `realStock < quantity` 校验兜底，不会超卖；现网用户量级为单摊位顾客，风险可接受。
- [菜单接口语义变化（stock 字段含义变为真实可售）对其他调用方的影响] → 该接口仅小程序菜单页调用（已确认 `getStallMenuAPI` 是唯一消费方），无其他调用方。
- [每商品一次 SKU 查询的 N+1] → 菜单商品量级小（摊位场景几十以内）；批量查询列为可选优化，不在本次范围。
- [提醒条查询失败/会话过期] → 静默降级为不展示，不影响菜单主流程。
- [奶油色系下价格/按钮对比度] → 强调色 `#D9622B` 在白卡上的对比度需实现时目测确认（按钮为白字on橙棕底，价格文字用强调色加深一档可保对比度）。

## Migration Plan

后端先行（接口向后兼容：字段不变、仅语义修正 + 不再过滤），前端随后发布。回滚：前端回退到上一版即可（旧前端对过滤后列表照常工作），后端可独立回滚。

## Open Questions

（无）
