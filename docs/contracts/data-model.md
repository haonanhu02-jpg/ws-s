# 数据模型基线

## 关联键

`visit_id` 是登记、OA、门卫、宿舍和后台投影的唯一业务关联键；各服务使用自己的技术主键。

## 独立状态

- 登记：`REGISTERED / CANCELLED / EXPIRED`
- OA：`NOT_STARTED / PROCESSING / APPROVED / REJECTED / FAILED`
- 门卫：`WAITING_ENTRY / IN_FACTORY / EXITED`
- 宿舍：`NOT_REQUIRED / PENDING_CONFIRMATION / PENDING_BED / BED_ASSIGNED / CHECKED_IN / CHECKED_OUT`

宿舍状态、撤销和过期规则尚未冻结。不得以一个总状态字段替代这些独立状态。

