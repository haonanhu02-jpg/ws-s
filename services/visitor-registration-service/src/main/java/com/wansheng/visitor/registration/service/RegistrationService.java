package com.wansheng.visitor.registration.service;

import com.wansheng.visitor.registration.api.CreateRegistrationRequest;
import com.wansheng.visitor.registration.config.RegistrationProperties;
import com.wansheng.visitor.registration.domain.OaStatus;
import com.wansheng.visitor.registration.domain.Registration;
import com.wansheng.visitor.registration.domain.RegistrationStatus;
import com.wansheng.visitor.registration.repository.OutboxRepository;
import com.wansheng.visitor.registration.repository.OutboxRepository.OutboxEvent;
import com.wansheng.visitor.registration.repository.RegistrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final RegistrationRepository registrations;
    private final OutboxRepository outbox;
    private final RegistrationValidator validator;
    private final RegistrationProperties properties;
    private final Clock clock;

    @Autowired
    public RegistrationService(
            RegistrationRepository registrations,
            OutboxRepository outbox,
            RegistrationValidator validator,
            RegistrationProperties properties) {
        this(registrations, outbox, validator, properties, Clock.systemUTC());
    }

    RegistrationService(
            RegistrationRepository registrations,
            OutboxRepository outbox,
            RegistrationValidator validator,
            RegistrationProperties properties,
            Clock clock) {
        this.registrations = registrations;
        this.outbox = outbox;
        this.validator = validator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Registration create(CreateRegistrationRequest request) {
        validator.validate(request, properties.phoneNotificationMode());
        Instant now = clock.instant();
        Registration registration = new Registration(
                id("VISIT"), request.visitorName().trim(), request.mobile().trim(),
                request.hostName().trim(),
                request.hostDepartment().trim(), request.visitReason().trim(),
                request.accommodationRequired(), request.hasVehicle(), normalize(request.plateNumber()),
                request.vehicleEnteringFactory(), Boolean.TRUE.equals(request.phoneNotificationRequested()),
                RegistrationStatus.REGISTERED, OaStatus.NOT_STARTED, now);
        registrations.insert(registration);
        outbox.insert(new OutboxEvent(0, id("EVENT"), "VISITOR_REGISTERED", registration.visitId(), now, 1));
        return registration;
    }

    public Optional<Registration> find(String visitId) {
        return registrations.findByVisitId(visitId);
    }

    public List<Registration> findRecent(int limit) {
        return registrations.findRecent(Math.max(1, Math.min(limit, 200)));
    }
    @Transactional
    public void updateOaStatus(String visitId, OaStatus status) {
        if (registrations.updateOaStatus(visitId, status) != 1) throw new IllegalArgumentException("registration not found");
        Instant now = clock.instant();
        outbox.insert(new OutboxEvent(0, id("EVENT"), "OA_APPROVAL_UPDATED", visitId, now, 1));
    }

    private static String id(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
