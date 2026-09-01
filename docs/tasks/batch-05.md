# BATCH-05 总后台、门户与验收

- `task_id`：BATCH-05
- 目标：提供只读综合查询、统计、审计和 `/visitor/` 门户交付基线。
- 规则：RULE-010～012；受 Q-005、Q-014、Q-015 约束。
- API：dashboard、registrations、guard-records、dormitory-records、audit-logs。
- 权限：`ADMIN`、`SYSTEM_ADMIN`；内部查询使用服务令牌。
- 不做：OA 后台展示、业务修正、数据自动删除、未经资料确认的企业 SSO。
- 验收：角色菜单和后端权限、敏感字段隔离、构建、Compose、`/visitor/` 路径均通过。

