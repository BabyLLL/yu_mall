# Tasks

## 1. 数据库与基础模型

- [x] 1.1 在 mall-mbg 的 OmsOrder 模型与 mapper XML 中新增 pickup_code 字段，并编写 SQL：`ALTER TABLE oms_order ADD COLUMN pickup_code varchar(16) NULL`（SQL 存入 mall 文档目录），验证 `mvn compile -pl mall-mbg` 通过
- [x] 1.2 在 mall-portal 定义摊位订单状态常量类（0=已下单,1=已付款,2=制作中,3=待取货,4=已完成,5=已取消）与 orderType=2 摊位订单标记，验证常量被后续任务引用

## 2. 取货码与摊位下单后端

- [x] 2.1 实现取货码生成器：Redis `INCR stall:pickup:code:yyyyMMdd`，格式 `N%03d`，key TTL 48h；单测覆盖当日递增与跨天重置（mock Redis 或嵌入式验证），测试通过
- [x] 2.2 实现 StallOrderService.create：入参 `[{productId, quantity}] + note`，校验上架状态（publish_status=1、未删除）与库存，按现价计算金额，复用/提取库存锁定逻辑，写 pickup_code，发送延迟取消消息（超时时长配置化，默认 30 分钟）；单测覆盖成功下单、商品下架失败、库存不足失败三个场景，测试通过
- [x] 2.3 实现 StallOrderController：`POST /stall/order/create`（会员鉴权）、`GET /stall/order/list`（含 pickup_code、状态过滤）、`POST /stall/order/{id}/cancel`（仅已下单状态可取消）；Swagger 手动验证三接口行为（代码与单测完成；Swagger 实例验证待后端联调时执行）
- [x] 2.4 调整超时取消消费者：目标状态改为 5(已取消) 并释放库存，仅作用于摊位订单（orderType=2），验证延迟消息触发后订单状态与库存正确（cancelOrder 与 cancelTimeOutOrder 均按 orderType 分流；延迟触发验证待后端联调执行）

## 3. 摊主模式后端

- [x] 3.1 鉴权 spike：验证 authType=admin 的 token 调用 mall-portal 新接口的放行方式（JwtAuthenticationTokenFilter 的 expected-type 处理 / @PreAuthorize MINIAPP_ADMIN），确定方案后落配置，curl 用 admin token 验证 200、member token 验证 403（方案：filter 放行 admin 会话认证 + @EnableMethodSecurity + @PreAuthorize('MINIAPP_ADMIN')；安全模块单测通过；curl 实例验证待联调）
- [x] 3.2 实现 `POST /stall/vendor/product/hide|show`（置 publish_status 0/1）与 `GET /stall/vendor/products`（含已隐藏商品）；Swagger 验证隐藏后 `/stall/menu/list` 不再返回（实现为 /product/{id}/hide|show；Swagger 实例验证待联调）
- [x] 3.3 实现摊主订单接口：`GET /stall/vendor/orders?status=` 与 `POST /stall/vendor/order/{id}/confirmPay|startMaking|finishMaking|cancel`，状态流转校验（非法流转拒绝），记录 paymentTime/makingTime 等时间戳；Swagger 验证完整流转 已下单→已付款→制作中→待取货→已完成（流转校验基于 VENDOR_TRANSITIONS；Swagger 实例验证待联调）

## 4. 菜单接口（白名单）

- [x] 4.1 实现 `GET /stall/menu/list`：返回上架且未删除、未售罄商品（pic/name/price/sale），加入 secure.ignored 白名单；curl 无 token 验证 200（白名单已加；curl 实例验证待联调）

## 5. 前端：菜单页与下单流程

- [x] 5.1 改造 pages.json：tabBar 收敛为 2 个（今日菜单 pages/index/index、我的订单 pages/order/order），注册新页面 pages/stall/confirm-order、pages/stall/order-success、pages/vendor/index，移除已砍页面的注册项；`npm run dev:mp-weixin` 构建通过
- [x] 5.2 新增 Pinia store：已选清单（商品/数量/合计），并新增 `apis/stall.ts`（menu/list、order/create、order/list、cancel、vendor 系列）；类型检查 `npm run tsc` 通过
- [x] 5.3 重写 pages/index/index.vue 为菜单页：菜单网格卡片（图/名/价/+按钮）、底部常驻已选条（件数/合计/去下单）、点击弹出已选清单可增减删除、售罄样式不可加单；H5/开发者工具中手动验证加单→合计更新→清空流程（代码完成；开发者工具验证待联调）
- [x] 5.4 新增确认下单页 confirm-order.vue：清单、合计、备注输入、提交调用 order/create；未登录先引导微信登录且已选不丢失，空清单禁止提交；下单成功跳转 order-success；手动验证两条路径（代码完成；开发者工具验证待联调）
- [x] 5.5 新增下单成功页 order-success.vue：大字取货码 + 应付金额 + 提示到摊叫号；改造订单列表/详情页：进行中订单大字常驻取货码与状态文案（已下单/已付款/制作中/待取货/已完成/已取消），已下单状态可取消、制作中取消被拒提示；手动验证取货码展示与取消行为（代码完成；开发者工具验证待联调）

## 6. 前端：摊主模式

- [x] 6.1 user 页加入隐藏入口（连续多次点击触发）弹出账号密码登录，调用已有 adminLoginAPI，成功后跳转 pages/vendor/index；手动验证登录成功与失败提示（已有隐藏入口（连点5次头像）+ admin 登录链路复用；新增摊主会话下的"摊主模式"入口；开发者工具验证待联调）
- [x] 6.2 实现 pages/vendor/index.vue 两个分段：商品管理（列表含已隐藏 + 上架/隐藏开关调 vendor/product/show|hide）与订单队列（按状态分组，确认收款/开始制作/出餐完成/取消按钮对应 vendor 接口，展示取货码/清单/金额/备注）+ 退出摊主模式；手动验证隐藏商品后菜单页消失、完整订单队列流转（代码完成；开发者工具验证待联调）

## 7. 清理与端到端验证

- [x] 7.1 移除入口并清理引用：首页旧楼层（秒杀/新鲜好物/人气推荐/猜你喜欢）、分类 tab、购物车 tab、优惠券/品牌/地址/支付页跳转、user 页足迹/收藏/关注入口；相关页面文件与 apis 保留不删；`npm run tsc` 与 `npm run dev:mp-weixin` 通过
- [ ] 7.2 端到端验证：未登录→加单→登录→提交→取货码→摊主确认收款→开始制作→出餐完成→顾客查看"请取货"→（摊主或顾客视角）完成；另验证未付款超时自动取消与库存释放；全程在微信开发者工具 + 本地后端通过
