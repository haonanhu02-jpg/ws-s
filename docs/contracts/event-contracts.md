# 事件契约基线

所有事件都使用统一信封：

```json
{
  "eventId": "EVENT-...",
  "eventType": "VISITOR_REGISTERED",
  "visitId": "VISIT-...",
  "occurredAt": "2026-08-24T00:00:00Z",
  "version": 1
}
```

首版事件：`VISITOR_REGISTERED`、`MANUAL_VISITOR_REGISTERED`、`OA_APPROVAL_UPDATED`、`VISITOR_ENTERED`、`VISITOR_EXITED`。

`MANUAL_VISITOR_REGISTERED` 保留旧名称与 routing key `guard.manual-visitor-registered` 以兼容已部署队列。
它在门卫手工创建且选择住宿时、所有授权资料修改时（包括取消住宿）、历史投影修复时产生；
只携带统一最小信封。宿舍读取 `/internal/guard/records/{id}/dormitory-view` 的最新快照与
`detailsVersion`，二维码来源的部门/事由从登记服务宿舍视图获取。信封 `version` 是契约版本，
不是资料版本。宿舍事务内按 eventId 幂等并按 detailsVersion 拒绝过期快照。

取消与确认并发时，宿舍行锁及确认/床位检查优先保护已确认住宿；通过内部接口按资料版本
拒绝门卫取消（恢复“需要住宿”并写审计和新 Outbox 事件）。这是最终一致性补偿，
不是跨服务原子事务；失败时消息重试，绝不自动释放床位。正常编辑前同步校验返回 409；
宿舍服务不可用返回 503。未确认取消保留内部记录以防旧消息复活，但不在住宿列表展示。

`OA_APPROVAL_UPDATED` 仍只携带统一最小信封；门卫收到事件后使用 `visit_id` 调用 `guard-view` 获取最新只读 OA 状态，事件本身不传播审批详情。

消费者必须按 `eventId` 幂等；生产者使用 Outbox；身份证及完整登记数据禁止进入普通事件。
