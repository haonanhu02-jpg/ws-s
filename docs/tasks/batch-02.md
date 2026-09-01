# BATCH-02 二维码登记

- `task_id`：BATCH-02
- 目标：完成二维码登记、完整主数据、`visit_id`、角色隔离视图和可靠事件触发。
- 依赖：RULE-001、002、003、005、008、013、014、017；Q-001～004、Q-006～007、Q-011。
- 服务与目录：`visitor-registration-service`、`frontend/visitor-web`、登记契约文档。
- API/事件/数据：公共登记 API、结果 API、三种内部 DTO、`VISITOR_REGISTERED` Outbox。
- 实现：条件校验、事务保存、最小事件、内部令牌、登记 H5。
- 测试：字段校验、Q-006 三态门禁、DTO 隔离、数据库及 Outbox、前端构建。
- 验收：提交产生唯一 `visit_id` 和一条未发布事件；门卫 DTO 无敏感字段。
- 不做：真实 OA、电话任务、重复访客合并、身份证展示策略、撤销及过期。
- 回滚：回退应用并回滚 `registration_schema` 的 V1 迁移（仅限无生产数据环境）。
- 完成门禁：全量 Maven、前端、Compose 校验通过；Q-* 未被默认固化。

