package com.wansheng.visitor.oa.mock;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("oa-mock")
class MockOaConfiguration {
    @Bean
    TopicExchange visitorEventsExchange() {
        return new TopicExchange("visitor.events", true, false);
    }

    @Bean
    Queue oaRegistrationQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable("oa.visitor-registered")
                .deadLetterExchange("").deadLetterRoutingKey("oa.visitor-registered.dlq").build();
    }
    @Bean Queue oaRegistrationDeadLetterQueue() { return new Queue("oa.visitor-registered.dlq", true); }

    @Bean
    Binding oaRegistrationBinding(Queue oaRegistrationQueue, TopicExchange visitorEventsExchange) {
        return BindingBuilder.bind(oaRegistrationQueue).to(visitorEventsExchange).with("visitor.registered");
    }

    @Bean
    RestClient registrationRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
