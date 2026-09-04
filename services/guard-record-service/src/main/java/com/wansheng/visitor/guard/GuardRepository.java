package com.wansheng.visitor.guard;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class GuardRepository {
    private final JdbcTemplate jdbc;
    GuardRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    void createIfAbsent(String eventId, GuardRecord r, Instant now) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM guard_processed_event WHERE event_id=?", Integer.class, eventId) > 0) return;
        jdbc.update("INSERT INTO guard_record(visit_id,visitor_name,mobile,host_name,plate_number," +
                "vehicle_entering_factory,accommodation_required,oa_status,guard_status,created_at) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                r.visitId(),r.visitorName(),r.mobile(),r.hostName(),r.plateNumber(),r.vehicleEnteringFactory(),
                r.accommodationRequired(),r.oaStatus(),GuardStatus.WAITING_ENTRY.name(),Timestamp.from(now));
        jdbc.update("INSERT INTO guard_processed_event(event_id,processed_at) VALUES(?,?) ON CONFLICT DO NOTHING", eventId, Timestamp.from(now));
    }

    void createManual(GuardRecord r, String operator, Instant now) {
        jdbc.update("INSERT INTO guard_record(visit_id,visitor_name,mobile,host_name,plate_number," +
                        "vehicle_entering_factory,accommodation_required,oa_status,guard_status,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                r.visitId(), r.visitorName(), r.mobile(), r.hostName(), r.plateNumber(),
                r.vehicleEnteringFactory(), r.accommodationRequired(), r.oaStatus(), r.guardStatus().name(), Timestamp.from(now));
        audit(r.visitId(), "MANUAL_CREATED", operator, now);
        if (r.accommodationRequired()) addOutbox("MANUAL_VISITOR_REGISTERED", r.visitId(), now);
    }

    int updateDetails(String visitId, GuardRecordRequest request, String operator, Instant now) {
        int changed = jdbc.update("UPDATE guard_record SET visitor_name=?,mobile=?,host_name=?,plate_number=?," +
                        "vehicle_entering_factory=?,accommodation_required=?,version=version+1,details_version=details_version+1 WHERE visit_id=?",
                request.visitorName().trim(), request.mobile().trim(), request.hostName().trim(),
                normalize(request.plateNumber()), request.vehicleEnteringFactory(), request.accommodationRequired(), visitId);
        if (changed == 1) audit(visitId, "DETAILS_UPDATED", operator, now);
        if (changed == 1) addOutbox("MANUAL_VISITOR_REGISTERED", visitId, now);
        return changed;
    }

    Optional<GuardRecord> find(String visitId) { return jdbc.query("SELECT * FROM guard_record WHERE visit_id=?",this::map,visitId).stream().findFirst(); }
    List<String> pendingAccommodationSync(){return jdbc.queryForList("SELECT visit_id FROM guard_record WHERE accommodation_sync_pending=TRUE ORDER BY created_at LIMIT 25",String.class);}
    @org.springframework.transaction.annotation.Transactional
    void reconcileAccommodation(String id,Boolean originalRequired,Instant now){
        if(originalRequired!=null)jdbc.update("UPDATE guard_record SET accommodation_required=?,details_version=details_version+1 WHERE visit_id=? AND details_version=0",originalRequired,id);
        if(jdbc.update("UPDATE guard_record SET accommodation_sync_pending=FALSE WHERE visit_id=? AND accommodation_sync_pending=TRUE",id)==1)addOutbox("MANUAL_VISITOR_REGISTERED",id,now);
    }
    Optional<GuardDormitoryView> dormitoryView(String id) {
        return jdbc.query("SELECT * FROM guard_record WHERE visit_id=?", (r,n) -> new GuardDormitoryView(r.getString("visit_id"),r.getString("visitor_name"),r.getString("mobile"),r.getString("host_name"),r.getBoolean("accommodation_required"),r.getString("plate_number"),r.getBoolean("vehicle_entering_factory"),r.getLong("details_version")),id).stream().findFirst();
    }
    void rejectCancellation(String id,long version,Instant now) {
        if(jdbc.update("UPDATE guard_record SET accommodation_required=TRUE,details_version=details_version+1 WHERE visit_id=? AND details_version=? AND accommodation_required=FALSE",id,version)==1){
            audit(id,"CANCELLATION_REJECTED_BY_DORMITORY","dormitory-service",now);
            addOutbox("MANUAL_VISITOR_REGISTERED",id,now);
        }
    }
    List<GuardRecord> list(GuardStatus status) { return jdbc.query("SELECT * FROM guard_record WHERE guard_status=? ORDER BY created_at DESC",this::map,status.name()); }
    List<GuardRecord> listAll(){return jdbc.query("SELECT * FROM guard_record ORDER BY created_at DESC",this::map);}
    List<java.util.Map<String,Object>> audits(){return jdbc.query("SELECT visit_id,action,operator_name,operated_at FROM guard_audit_log ORDER BY operated_at DESC LIMIT 200",(r,n)->java.util.Map.of("visitId",r.getString(1),"action",r.getString(2),"operator",r.getString(3),"operatedAt",r.getTimestamp(4).toInstant()));}
    void updateOaStatus(String visitId,String status){jdbc.update("UPDATE guard_record SET oa_status=? WHERE visit_id=?",status,visitId);}

    int transition(String visitId, GuardStatus from, GuardStatus to, String operator, Instant now) {
        String timeColumn = to == GuardStatus.IN_FACTORY ? "entry_time" : "exit_time";
        String operatorColumn = to == GuardStatus.IN_FACTORY ? "entry_operator" : "exit_operator";
        int changed = jdbc.update("UPDATE guard_record SET guard_status=?,"+timeColumn+"=?,"+operatorColumn+"=?,version=version+1 WHERE visit_id=? AND guard_status=?",
                to.name(),Timestamp.from(now),operator,visitId,from.name());
        if (changed == 1) {
            audit(visitId, to.name(), operator, now);
            String eventType=to==GuardStatus.IN_FACTORY?"VISITOR_ENTERED":"VISITOR_EXITED";
            addOutbox(eventType, visitId, now);
        }
        return changed;
    }

    List<GuardOutboxEvent> unpublished(){return jdbc.query("SELECT id,event_id,event_type,visit_id,occurred_at FROM guard_outbox WHERE published_at IS NULL ORDER BY id LIMIT 100",(r,n)->new GuardOutboxEvent(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getTimestamp(5).toInstant()));}
    void published(long id,Instant at){jdbc.update("UPDATE guard_outbox SET published_at=? WHERE id=?",Timestamp.from(at),id);}
    record GuardOutboxEvent(long id,String eventId,String eventType,String visitId,Instant occurredAt){}

    private void audit(String visitId, String action, String operator, Instant now) {
        jdbc.update("INSERT INTO guard_audit_log(visit_id,action,operator_name,operated_at) VALUES(?,?,?,?)",
                visitId, action, operator, Timestamp.from(now));
    }

    private void addOutbox(String eventType, String visitId, Instant now) {
        jdbc.update("INSERT INTO guard_outbox(event_id,event_type,visit_id,occurred_at) VALUES(?,?,?,?)",
                "EVENT-"+UUID.randomUUID().toString().replace("-","").toUpperCase(),eventType,visitId,Timestamp.from(now));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private GuardRecord map(ResultSet rs,int n)throws SQLException { return new GuardRecord(rs.getString("visit_id"),rs.getString("visitor_name"),rs.getString("mobile"),rs.getString("host_name"),rs.getString("plate_number"),rs.getBoolean("vehicle_entering_factory"),rs.getBoolean("accommodation_required"),rs.getString("oa_status"),GuardStatus.valueOf(rs.getString("guard_status")),instant(rs,"entry_time"),instant(rs,"exit_time"),rs.getString("entry_operator"),rs.getString("exit_operator")); }
    private static Instant instant(ResultSet rs,String c)throws SQLException { Timestamp t=rs.getTimestamp(c); return t==null?null:t.toInstant(); }
}
