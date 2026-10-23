# 微信小程序双身份登录发布手册

## 服务入口与配置

小程序只连接 `mall-portal`，不得配置或调用 `mall-admin` 地址。生产环境仅需为前端设置 `VITE_API_BASE_URL`，并由该门户地址提供以下接口：

- `POST /sso/wechat/login`：微信临时 code 换取会员会话。
- `POST /sso/miniapp/admin/login`：隐藏入口的管理员账号密码登录。
- `POST /sso/miniapp/logout`：会员和管理员统一显式退出。

`mall-portal` 使用以下环境变量：

- `WECHAT_MINIAPP_ENABLED`：微信会员登录开关，初始发布时设为 `false`。
- `WECHAT_MINIAPP_APP_ID`：微信小程序 App ID。
- `WECHAT_MINIAPP_APP_SECRET`：微信小程序 App Secret。
- `MINIAPP_SESSION_KEY_PREFIX`：可选，默认 `mall:miniapp:session`。

启用微信登录时 App ID 或 App Secret 缺失会阻止门户启动。Redis 会话 TTL 直接使用 `jwt.expiration`，两者必须保持一致。

## 发布顺序

1. 备份数据库，在商城库执行 `document/sql/mall_wechat_auth.sql`；确认 `ums_member` 行数不变，并验证 `(app_id, openid)` 唯一约束。
2. 部署包含身份表映射、原子会话注册表和三个门户接口的后端，保持 `WECHAT_MINIAPP_ENABLED=false`。
3. 运行后端测试，验证后台 Web 的 `/admin/login` 契约不变；小程序不再访问该接口。
4. 发布小程序，验证会员底部授权弹层、头像五击管理员入口、单会话阻断和显式退出。
5. 配置微信凭证并设置 `WECHAT_MINIAPP_ENABLED=true`，逐步放量观察指标。

## 会话与错误处理

登录请求携带持久化的非凭证 `clientId`。Redis 使用 `SET NX` 为每个 clientId 创建唯一活动会话；任何已有会员或管理员会话都会返回 `409 LOGIN_CONFLICT`，原会话和 TTL 保持不变。客户端收到该结果必须保留当前 `authSession` 并提示先退出。

退出顺序固定为：客户端携带当前 token、clientId 和 sessionId 调用门户；门户原子比对并删除活动记录；只有成功响应后客户端才清除本地会话。标识不匹配返回 `40901 SESSION_MISMATCH`，客户端保留会话并允许重试退出。网络失败同样不得先清理本地状态；明确的 401 表示会话已失效，可以清理一次并回到对应恢复流程。

## 验证清单

- 游客仍可浏览公开商品，受保护操作在当前页打开微信授权底部弹层。
- 每次确认或重试都会获取新的微信临时 code；成功后先保存会话，再关闭弹层并只恢复一次待执行操作。
- Redis 出现 `<MINIAPP_SESSION_KEY_PREFIX>:<clientId>`，TTL 与 JWT 过期时间一致。
- 相同身份重复登录、会员/管理员交叉登录和并发登录均只允许一个会话成功。
- 错误 sessionId 退出返回 `40901` 且 Redis 记录仍存在；正确退出后才可重新登录。
- 头像五次点击的任意相邻间隔超过一秒会重置；已有会话时不会打开管理员表单。
- 已退出、过期、缺少会话元数据或身份域错误的 token 无法访问受保护接口。

## 监控与日志

监控微信凭证交换失败率、`409` 重复登录次数、`40901` 退出不匹配次数、401 数量、Redis `SET NX`/比对删除失败和管理员登录失败率。日志可记录 clientId 后六位、身份类型和结果，但不得记录完整 clientId、token、openid、微信 code 或 secret。

## 回滚

1. 先设置 `WECHAT_MINIAPP_ENABLED=false`，停止新的微信会员登录；公开浏览、隐藏管理员登录和既有后台 Web 登录不受影响。
2. 回滚小程序版本，再按兼容窗口回滚门户代码；必要时清理 `MINIAPP_SESSION_KEY_PREFIX:*`，该操作会让现有小程序会话失效。
3. 身份表默认保留以便重新发布。只有确认不再需要映射并完成备份后，才执行脚本末尾的 `DROP TABLE IF EXISTS ums_member_wechat_identity` 回滚语句；该操作不删除 `ums_member`。
