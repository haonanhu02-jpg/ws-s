# 员工宿舍 8.5 后端契约（阶段 1～4）

## 页面冻结范围

本契约适配 `D:\download\宿舍管理系统\8.5` 现有页面，不改变页面结构、字段、菜单和布局。
前端原 `DM.data.regions[*].rooms[*].beds[*].occupant` 数据改由后端资源树和住宿接口提供。

## 资源模型

- 楼栋：名称、所属区域、启用状态、排序。
- 房间：楼栋、楼层、房号、朝向、房型、是否可住、清洁状态、平面图网格坐标、备注。
- 床位：房间、标签、编码、启用状态、三件套、待打扫状态。住宿记录勾选待打扫时同步标记床位，退宿后仍保留，直至管理员点击“完成打扫”。
- 人员：姓名、中心、部门、性别、人员类别、岗位、职级。
- 住宿：申请单编码、对接人、床位类型、三件套（自带/公司提供/空白）、三件套说明、是否纳入降本、承诺书、床位待打扫状态、入住/退房水电读数、计划/实际入住退宿日期和备注。

人员类别允许空白；可选值为：基地学习人员、总部临时入住人员、外部合作入住人员、新员工（派驻基地过渡）、新员工(普通)、己审批长住员工。

## 状态

员工住宿状态独立于访客住宿状态：

`BOOKED -> CHECKED_IN -> CHECKED_OUT`，`BOOKED -> CANCELLED`。

调宿和续住不改变当前入住状态。已预订或已入住记录只在其计划住宿日期内占用床位；同一床位可保存日期不重叠的连续未来预订，日期首尾均计入区间，同日交接视为冲突。标间不得在日期重叠期间男女混住。

## API

统一前缀：`/api/visitor/dormitory/employee`

- `GET /resources/tree`
- `POST /buildings`
- `PUT /buildings/{id}`
- `POST /rooms`
- `PUT /rooms/{id}`
- `POST /beds`
- `PUT /beds/{id}`
- `POST /beds/{id}/cleaning`（标记待打扫或完成打扫）
- `GET /people?name=`、`POST /people`、`PUT /people/{id}`
- `GET /people/{id}/stays`
- `GET /stays?status=&buildingId=&name=`、`GET /stays/{id}`
- `POST /stays/book`
- `PUT /stays/{id}`（编辑预订或在住人员及住宿信息）
- `POST /stays/{id}/check-in`
- `POST /stays/{id}/transfer`
- `POST /stays/{id}/extend`
- `POST /stays/{id}/check-out`
- `POST /stays/{id}/cancel`
- `DELETE /stays/{id}`（仅允许永久删除已取消或已退宿记录，同时清理附件与住宿操作明细）
- `DELETE /people/{id}`（仅允许删除不存在任何住宿记录的人员档案）
- `GET /stay-audits`
- `GET /resource-audits`
- `GET /meter-readings?month=YYYY-MM`
- `PUT /meter-readings`
- `POST /imports/people`（Excel 解析后的人员批量数据，事务导入）
- `POST /imports/resources`（Excel 解析后的楼栋、房间和床位批量数据，事务导入）
- `POST /imports/stays`（完整住宿模板批量导入，包含人员、床位和全部住宿字段）
- `GET /statistics`（床位、楼栋、人员类别和住宿状态统计）
- `GET/POST /stays/{id}/attachments`、`GET/DELETE /attachments/{id}`（住宿附件）
- `GET/PUT /fees/rule`（水电单价、每房免费额度和启用状态）
- `GET /fees/bills?month=`、`POST /fees/generate?month=`、`PUT /fees/bills/{id}`（房间月度账单）

所有写接口从 JWT 用户名取得操作人，不接受客户端伪造操作人。附件限制 10MB，仅允许 PDF、图片、Word、Excel，并保存到持久化卷；费用按房间月度抄表结算，账单保存生成时的规则和住宿人员快照，确认后不可修改。多人同住房费分摊规则尚未确认，因此不自动拆分到个人。前端完整住宿模板含全部可填写和必填字段；导入导出范围跟随顶部当前楼栋或全集团选择。住宿/退宿确认单打印，以及台账、人员、历史、抄表、房间费用明细和统计数据均可导出 Excel。
