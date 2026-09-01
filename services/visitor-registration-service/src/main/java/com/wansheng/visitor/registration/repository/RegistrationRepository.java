package com.wansheng.visitor.registration.repository;

import com.wansheng.visitor.registration.domain.OaStatus;
import com.wansheng.visitor.registration.domain.Registration;
import com.wansheng.visitor.registration.domain.RegistrationStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegistrationRepository {
    private final JdbcTemplate jdbc;

    public RegistrationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Registration registration) {
        jdbc.update("""
                INSERT INTO visitor_registration (
                  visit_id, visitor_name, mobile, host_name, host_department,
                  visit_reason, accommodation_required, has_vehicle, plate_number,
                  vehicle_entering_factory, phone_notification_requested, registration_status,
                  oa_status, registered_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                registration.visitId(), registration.visitorName(), registration.mobile(),
                registration.hostName(), registration.hostDepartment(),
                registration.visitReason(), registration.accommodationRequired(), registration.hasVehicle(),
                registration.plateNumber(), registration.vehicleEnteringFactory(),
                registration.phoneNotificationRequested(), registration.registrationStatus().name(),
                registration.oaStatus().name(), Timestamp.from(registration.registeredAt()),
                Timestamp.from(registration.registeredAt()));
    }

    public Optional<Registration> findByVisitId(String visitId) {
        return jdbc.query("SELECT * FROM visitor_registration WHERE visit_id = ?", this::map, visitId)
                .stream().findFirst();
    }

    public List<Registration> findRecent(int limit) {
        return jdbc.query("""
                SELECT * FROM visitor_registration
                ORDER BY registered_at DESC, id DESC
                LIMIT ?
                """, this::map, limit);
    }
    public int updateOaStatus(String visitId, OaStatus status) {
        return jdbc.update("UPDATE visitor_registration SET oa_status = ? WHERE visit_id = ?", status.name(), visitId);
    }

    private Registration map(ResultSet rs, int rowNumber) throws SQLException {
        return new Registration(
                rs.getString("visit_id"), rs.getString("visitor_name"), rs.getString("mobile"),
                rs.getString("host_name"),
                rs.getString("host_department"), rs.getString("visit_reason"),
                rs.getBoolean("accommodation_required"), rs.getBoolean("has_vehicle"),
                rs.getString("plate_number"), rs.getBoolean("vehicle_entering_factory"),
                rs.getBoolean("phone_notification_requested"),
                RegistrationStatus.valueOf(rs.getString("registration_status")),
                OaStatus.valueOf(rs.getString("oa_status")),
                rs.getTimestamp("registered_at").toInstant());
    }
}
