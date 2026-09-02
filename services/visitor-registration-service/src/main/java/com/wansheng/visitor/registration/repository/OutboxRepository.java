package com.wansheng.visitor.registration.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {
    private final JdbcTemplate jdbc;

    public OutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(OutboxEvent event) {
        jdbc.update("""
                INSERT INTO registration_outbox
                  (event_id, event_type, visit_id, occurred_at, event_version)
                VALUES (?, ?, ?, ?, ?)
                """, event.eventId(), event.eventType(), event.visitId(),
                Timestamp.from(event.occurredAt()), event.version());
    }

    public List<OutboxEvent> findUnpublished(int limit) {
        return jdbc.query("""
                SELECT id, event_id, event_type, visit_id, occurred_at, event_version
                FROM registration_outbox
                WHERE published_at IS NULL
                ORDER BY id
                LIMIT ?
                """, this::map, limit);
    }

    public void markPublished(long id, Instant publishedAt) {
        jdbc.update("UPDATE registration_outbox SET published_at = ?, last_error = NULL WHERE id = ?",
                Timestamp.from(publishedAt), id);
    }

    public void markFailed(long id, String error) {
        jdbc.update("""
                UPDATE registration_outbox
                SET retry_count = retry_count + 1, last_error = ?
                WHERE id = ?
                """, error == null ? "unknown publish error" : error.substring(0, Math.min(error.length(), 1000)), id);
    }

    private OutboxEvent map(ResultSet rs, int rowNumber) throws SQLException {
        return new OutboxEvent(
                rs.getLong("id"), rs.getString("event_id"), rs.getString("event_type"),
                rs.getString("visit_id"), rs.getTimestamp("occurred_at").toInstant(),
                rs.getInt("event_version"));
    }

    public record OutboxEvent(
            long id, String eventId, String eventType, String visitId, Instant occurredAt, int version) {
    }
}
