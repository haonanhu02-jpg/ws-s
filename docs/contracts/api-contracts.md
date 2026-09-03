# API 契约索引

统一前缀：`/api/visitor`。正式业务开发前以 OpenAPI 文件冻结请求、响应和错误码。

| 领域 | 端点 |
|---|---|
| 登记 | `POST /registrations`、`GET /registrations/{visitId}/result` |
| 门卫 | `GET /guard/records`、`POST /guard/records`、`PUT /guard/records/{visitId}`、`POST /guard/records/{visitId}/entry`、`POST /guard/records/{visitId}/exit` |
| 宿舍 | `/dormitory/records` 下的查询、确认、床位、入住退宿接口 |
| 后台 | `/admin/dashboard`、登记/门卫/宿舍/审计查询 |
| 认证 | `POST /auth/login`、`GET /auth/me` |
| 系统管理 | `/auth/users` 下的账号查询、创建、更新、重置密码和删除 |

门卫记录在二维码登记提交成功后立即可见。宿舍记录查询只返回勾选“需要住宿”的
访客登记；未勾选住宿的登记不进入宿舍处理范围。

门卫可手工创建记录，也可修改姓名、手机号、被访人、车牌号和车辆是否进厂等
门卫授权字段。手工记录使用 `GUARD-` 前缀的业务编号，初始状态为 `WAITING_ENTRY`，
OA 状态为 `NOT_STARTED`；创建和修改均记录操作审计。

内部管理页面统一使用登录接口签发的 8 小时 `Bearer` Token。用户管理接口仅
`SYSTEM_ADMIN` 可访问；系统必须保留至少一个启用的系统管理员，当前账号不能
停用或删除。支持角色：`GUARD`、`DORM_ADMIN`、`ADMIN`、`SYSTEM_ADMIN`。

内部最小权限视图：

- `/internal/registrations/{visitId}/guard-view`
- `/internal/registrations/{visitId}/dormitory-view`
- `/internal/registrations/{visitId}/oa-view`
