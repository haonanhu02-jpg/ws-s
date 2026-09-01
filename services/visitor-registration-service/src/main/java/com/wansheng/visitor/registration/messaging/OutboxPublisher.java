package com.wansheng.visitor.registration.messaging;

import com.wansheng.visitor.registration.repository.OutboxRepository;
import com.wansheng.visitor.registration.repository.OutboxRepository.OutboxEvent;
import java.time.Clock;
import java.time.Instant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OutboxPublisher {
    private final OutboxRepository outbox;
    private final RabbitTemplate rabbit;
    private final Clock clock = Clock.systemUTC();

    OutboxPublisher(OutboxRepository outbox, RabbitTemplate rabbit) {
        this.outbox = outbox;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${visitor.registration.outbox-delay-ms:1000}")
    void publishPending() {
        for (OutboxEvent event : outbox.findUnpublished(100)) {
            try {
                rabbit.convertAndSend(RegistrationMessagingConfiguration.EXCHANGE, routingKey(event), json(event));
                outbox.markPublished(event.id(), clock.instant());
            } catch (RuntimeException failure) {
                outbox.markFailed(event.id(), failure.getMessage());
            }
        }
    }

    private static String routingKey(OutboxEvent event) {
        return switch (event.eventType()) {
            case "VISITOR_REGISTERED" -> "visitor.registered";
            case "OA_APPROVAL_UPDATED" -> "oa.approval.updated";
            default -> "visitor.unknown";
        };
    }

    private static String json(OutboxEvent event) {
        return "{" +
                "\"eventId\":\"" + event.eventId() + "\"," +
                "\"eventType\":\"" + event.eventType() + "\"," +
                "\"visitId\":\"" + event.visitId() + "\"," +
                "\"occurredAt\":\"" + event.occurredAt() + "\"," +
                "\"version\":" + event.version() +
                "}";
    }
}
