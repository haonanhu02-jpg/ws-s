package com.wansheng.visitor.registration.api;

import com.wansheng.visitor.registration.config.RegistrationProperties;
import com.wansheng.visitor.registration.domain.Registration;
import com.wansheng.visitor.registration.service.RegistrationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/visitor/registrations")
public class RegistrationController {
    private final RegistrationService service;
    private final RegistrationProperties properties;

    public RegistrationController(RegistrationService service, RegistrationProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/configuration")
    Map<String, String> configuration() {
        return Map.of("phoneNotificationMode", properties.phoneNotificationMode().name());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResultResponse create(@Valid @RequestBody CreateRegistrationRequest request) {
        return result(service.create(request));
    }

    @GetMapping("/{visitId}/result")
    RegistrationResultResponse result(@PathVariable String visitId) {
        return result(service.find(visitId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "来访记录不存在")));
    }

    private static RegistrationResultResponse result(Registration registration) {
        return new RegistrationResultResponse(
                registration.visitId(), registration.registrationStatus().name(),
                registration.oaStatus().name(), registration.registeredAt(),
                "登记成功不代表获准通行，请等待现场确认");
    }
}
