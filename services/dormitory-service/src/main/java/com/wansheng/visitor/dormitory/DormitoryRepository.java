package com.wansheng.visitor.dormitory;
import java.sql.*; import java.time.Instant; import java.util.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Repository;
@Repository class DormitoryRepository{
 private final JdbcTemplate jdbc; DormitoryRepository(JdbcTemplate j){jdbc=j;}
 void createIfAbsent(String eid,DormitoryRecord r,Instant now){if(jdbc.queryForObject("SELECT COUNT(*) FROM dormitory_processed_event WHERE event_id=?",Integer.class,eid)>0)return;jdbc.update("INSERT INTO dormitory_record(visit_id,visitor_name,mobile,host_name,host_department,visit_reason,accommodation_required,has_vehicle,plate_number,vehicle_entering_factory,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",r.visitId(),r.visitorName(),r.mobile(),r.hostName(),r.hostDepartment(),r.visitReason(),r.accommodationRequired(),r.hasVehicle(),r.plateNumber(),r.vehicleEnteringFactory(),Timestamp.from(now));jdbc.update("INSERT INTO dormitory_processed_event VALUES(?,?) ON CONFLICT DO NOTHING",eid,Timestamp.from(now));}
 List<DormitoryRecord> list(boolean onlyAccommodation){return jdbc.query("SELECT r.*,b.bed_code FROM dormitory_record r LEFT JOIN dormitory_bed b ON b.id=r.bed_id "+(onlyAccommodation?"WHERE r.accommodation_required=TRUE ":"")+"ORDER BY r.created_at DESC",this::map);}
 List<Map<String,Object>> audits(){return jdbc.query("SELECT visit_id,old_bed_id,new_bed_id,operator_name,changed_at FROM bed_change_audit ORDER BY changed_at DESC LIMIT 200",(r,n)->{Map<String,Object>m=new LinkedHashMap<>();m.put("visitId",r.getString(1));m.put("oldBedId",r.getObject(2));m.put("newBedId",r.getObject(3));m.put("operator",r.getString(4));m.put("operatedAt",r.getTimestamp(5).toInstant());return m;});}
 Optional<DormitoryRecord> find(String id){return jdbc.query("SELECT r.*,b.bed_code FROM dormitory_record r LEFT JOIN dormitory_bed b ON b.id=r.bed_id WHERE r.visit_id=?",this::map,id).stream().findFirst();}
 int confirm(String id){return jdbc.update("UPDATE dormitory_record SET accommodation_confirmed=TRUE WHERE visit_id=? AND accommodation_required=TRUE AND accommodation_confirmed=FALSE",id);}
 boolean cancellationAllowed(String id){return find(id).map(r->!r.accommodationConfirmed()&&r.bedCode()==null).orElse(true);}
 // Caller owns the transaction. Row lock serializes this projection with dorm confirmation/bed updates.
 boolean syncGuard(String eid,DormitoryRecord r,long version,Instant now){
  if(jdbc.queryForObject("SELECT COUNT(*) FROM dormitory_processed_event WHERE event_id=?",Integer.class,eid)>0)return false;
  createIfAbsent(eid,r,now);
  long current=jdbc.queryForObject("SELECT guard_details_version FROM dormitory_record WHERE visit_id=? FOR UPDATE",Long.class,r.visitId());
  if(version<=current)return false;
  boolean rejected=!r.accommodationRequired()&&!cancellationAllowed(r.visitId());
  jdbc.update("UPDATE dormitory_record SET visitor_name=?,mobile=?,host_name=?,host_department=?,visit_reason=?,has_vehicle=?,plate_number=?,vehicle_entering_factory=?,accommodation_required=?,guard_details_version=? WHERE visit_id=?",r.visitorName(),r.mobile(),r.hostName(),r.hostDepartment(),r.visitReason(),r.hasVehicle(),r.plateNumber(),r.vehicleEnteringFactory(),r.accommodationRequired()||rejected,version,r.visitId());
  return rejected;
 }
 void addBed(String building,String room,String code){jdbc.update("INSERT INTO dormitory_bed(building_name,room_number,bed_code) VALUES(?,?,?)",building,room,code);}
 int assign(String visitId,String code,String op,Instant now){Long bed=jdbc.query("SELECT id FROM dormitory_bed WHERE bed_code=? AND enabled=TRUE",rs->rs.next()?rs.getLong(1):null,code);if(bed==null)return 0;Long old=jdbc.query("SELECT bed_id FROM dormitory_record WHERE visit_id=?",rs->rs.next()?(Long)rs.getObject(1):null,visitId);int n=jdbc.update("UPDATE dormitory_record SET bed_id=? WHERE visit_id=? AND accommodation_required=TRUE AND accommodation_confirmed=TRUE",bed,visitId);if(n==1)jdbc.update("INSERT INTO bed_change_audit(visit_id,old_bed_id,new_bed_id,operator_name,changed_at) VALUES(?,?,?,?,?)",visitId,old,bed,op,Timestamp.from(now));return n;}
 private DormitoryRecord map(ResultSet r,int n)throws SQLException{return new DormitoryRecord(r.getString("visit_id"),r.getString("visitor_name"),r.getString("mobile"),r.getString("host_name"),r.getString("host_department"),r.getString("visit_reason"),r.getBoolean("accommodation_required"),r.getBoolean("has_vehicle"),r.getString("plate_number"),r.getBoolean("vehicle_entering_factory"),r.getBoolean("accommodation_confirmed"),r.getString("bed_code"));}
}
