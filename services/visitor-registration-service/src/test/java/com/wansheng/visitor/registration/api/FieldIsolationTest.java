package com.wansheng.visitor.registration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FieldIsolationTest {
    @Test
    void guardViewNeverDeclaresSensitiveOrUnrelatedFields() {
        var names = Arrays.stream(GuardRegistrationView.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertThat(names).containsExactly(
                "visitId", "visitorName", "mobile", "hostName", "plateNumber",
                "vehicleEnteringFactory", "accommodationRequired", "oaStatus");
        assertThat(names).doesNotContain(
                "identityCardNumber", "hostDepartment", "visitReason",
                "phoneNotificationRequested");
    }
    @Test void adminViewDoesNotPrejudgeOaOrIdentityPermissions(){var names=Arrays.stream(AdminRegistrationView.class.getRecordComponents()).map(c->c.getName()).toList();assertThat(names).doesNotContain("identityCardNumber","oaStatus");}
}
