package com.wansheng.visitor.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AdminQueryControllerTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final ZoneId shanghai = ZoneId.of("Asia/Shanghai");

    @Test
    void dashboardUsesBusinessTimezoneAndCountsOnlyTodayExits() throws Exception {
        var registrations = json.readTree("""
                [{"registeredAt":"2026-09-02T16:30:00Z"},{"registeredAt":"2026-09-02T15:59:59Z"}]
                """);
        var guards = json.readTree("""
                [{"guardStatus":"EXITED","exitTime":"2026-09-02T16:10:00Z"},
                 {"guardStatus":"EXITED","exitTime":"2026-09-02T15:59:59Z"},
                 {"guardStatus":"WAITING_ENTRY"},{"guardStatus":"IN_FACTORY"}]
                """);
        var dormitory = json.readTree("[]");

        var result = AdminQueryController.calculateDashboard(
                registrations, guards, dormitory, LocalDate.of(2026, 9, 3), shanghai);

        assertThat(result.get("todayRegistrations")).isEqualTo(1);
        assertThat(result.get("exited")).isEqualTo(1);
        assertThat(result.get("waitingEntry")).isEqualTo(1);
        assertThat(result.get("inFactory")).isEqualTo(1);
    }
}
