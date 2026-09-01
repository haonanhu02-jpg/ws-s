# 万盛股份厂区访客管理平台

统一 Web 前端、API 网关和职责独立的后端微服务，用 `visit_id` 贯穿登记、OA、门卫、宿舍与后台查询。

## 工程结构

- `frontend/visitor-web`：Vue 3 管理端与访客登记 H5 壳
- `gateway/api-gateway`：统一 API 入口
- `services/*`：认证、登记、门卫、宿舍、OA、后台查询服务
- `common/*`：无业务实体的通用技术模块
- `deploy`：本地 Docker Compose 与 Nginx
- `docs`：需求、架构、契约和任务文档

## 本地验证

```bash
mvn clean verify
cd frontend/visitor-web
npm ci
npm run build
docker compose -f deploy/docker-compose.yml config
```

业务开发前先阅读 `docs/requirements/visitor-requirements-baseline.md` 和 `docs/requirements/open-questions.md`。

本地入口为 `http://localhost:8088/visitor/`；门户接入约束见 `docs/deployment/portal-integration.md`。
