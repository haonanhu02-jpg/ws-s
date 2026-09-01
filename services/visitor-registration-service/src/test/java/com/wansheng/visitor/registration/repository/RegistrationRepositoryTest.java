package com.wansheng.visitor.registration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wansheng.visitor.registration.domain.OaStatus;
import com.wansheng.visitor.registration.domain.Registration;
import com.wansheng.visitor.registration.domain.RegistrationStatus;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RegistrationRepositoryTest {
    private RegistrationRepository registrations;
    private OutboxRepository outbox;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        registrations = new RegistrationRepository(jdbc);
        outbox = new OutboxRepository(jdbc);
    }

    @Test
    void storesRegistrationAndMinimalOutboxEvent() {
        Instant now = Instant.parse("2026-08-24T00:00:00Z");
        Registration registration = new Registration(
                "VISIT-1", "张三", "13800138000", "李经理", "生产部", "交流",
                false, false, null, false, false,
                RegistrationStatus.REGISTERED, OaStatus.NOT_STARTED, now);
        registrations.insert(registration);
        outbox.insert(new OutboxRepository.OutboxEvent(
                0, "EVENT-1", "VISITOR_REGISTERED", "VISIT-1", now, 1));

        assertThat(registrations.findByVisitId("VISIT-1")).contains(registration);
        var event = outbox.findUnpublished(10).get(0);
        assertThat(event.visitId()).isEqualTo("VISIT-1");
        assertThat(event.eventType()).isEqualTo("VISITOR_REGISTERED");
    }
}
