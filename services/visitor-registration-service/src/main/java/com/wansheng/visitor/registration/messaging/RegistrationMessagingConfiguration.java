package com.wansheng.visitor.registration.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RegistrationMessagingConfiguration {
    static final String EXCHANGE = "visitor.events";

    @Bean
    TopicExchange visitorEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}

