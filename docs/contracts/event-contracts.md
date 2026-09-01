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

首版事件：`VISITOR_REGISTERED`、`OA_APPROVAL_UPDATED`、`VISITOR_ENTERED`、`VISITOR_EXITED`。

`OA_APPROVAL_UPDATED` 仍只携带统一最小信封；门卫收到事件后使用 `visit_id` 调用 `guard-view` 获取最新只读 OA 状态，事件本身不传播审批详情。

消费者必须按 `eventId` 幂等；生产者使用 Outbox；身份证及完整登记数据禁止进入普通事件。
