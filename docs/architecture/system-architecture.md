# 系统架构

```text
Visitor H5 / Unified Admin Web
              |
            Nginx
              |
         API Gateway
   +----------+----------+----------+----------+----------+
 Auth   Registration   Guard    Dormitory     OA       Admin Query
                         RabbitMQ events / internal least-privilege APIs
                                      |
                         PostgreSQL schemas + Redis
```

## 数据所有权

| 服务 | Schema | 所有数据 |
|---|---|---|
| auth-service | `auth_schema` | 用户、角色、权限 |
| visitor-registration-service | `registration_schema` | 登记主数据、敏感信息、电话模块 |
| guard-record-service | `guard_schema` | 门卫状态、进出时间、OA 只读投影 |
| dormitory-service | `dormitory_schema` | 住宿、房间、床位、调整历史 |
| oa-integration-service | `oa_integration_schema` | OA 映射、同步状态、调用日志 |
| admin-query-service | `admin_query_schema` | 综合只读投影、报表 |

跨服务仅通过 API 或事件通信，禁止跨 Schema 写表。普通事件只携带事件标识、类型、`visit_id`、时间和版本。

