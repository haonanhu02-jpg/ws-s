# 企业门户接入

- Web 基础路径：`/visitor/`
- API 前缀：`/api/visitor/`
- 公开登记：`/visitor/register`
- 内部登录：`/visitor/login`
- Nginx 已提供 SPA fallback 和 API 反向代理。

当前 Basic Auth 仅用于开发联调。企业门户提供 SSO 协议、用户标识、角色声明和登出地址后，应在 `auth-service` 与网关替换开发用户源；不得把开发密码用于生产。

