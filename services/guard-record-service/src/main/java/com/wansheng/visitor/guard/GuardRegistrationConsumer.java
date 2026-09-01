package com.wansheng.visitor.guard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Component
class GuardRegistrationConsumer {
 private final ObjectMapper json; private final RestClient client; private final GuardRepository repo; private final String url,token; private final Clock clock=Clock.systemUTC();
 GuardRegistrationConsumer(ObjectMapper json,RestClient.Builder builder,GuardRepository repo,@Value("${visitor.guard.registration-service-url}")String url,@Value("${visitor.guard.internal-token}")String token){this.json=json;client=builder.build();this.repo=repo;this.url=url;this.token=token;}
 @RabbitListener(queues="guard.visitor-registered") @Transactional void receive(String payload)throws Exception{
  JsonNode e=json.readTree(payload);String eventId=e.path("eventId").asText();String visitId=e.path("visitId").asText();if(eventId.isBlank()||visitId.isBlank())throw new IllegalArgumentException("invalid event");
  GuardView v=client.get().uri(url+"/internal/registrations/{id}/guard-view",visitId).header("X-Internal-Token",token).retrieve().body(GuardView.class);
  repo.createIfAbsent(eventId,new GuardRecord(v.visitId(),v.visitorName(),v.mobile(),v.hostName(),v.plateNumber(),v.vehicleEnteringFactory(),v.oaStatus(),GuardStatus.WAITING_ENTRY,null,null,null,null),clock.instant());
 }
 record GuardView(String visitId,String visitorName,String mobile,String hostName,String plateNumber,boolean vehicleEnteringFactory,String oaStatus){}
}

@Configuration
class GuardMessagingConfiguration {
 @Bean TopicExchange guardVisitorExchange(){return new TopicExchange("visitor.events",true,false);}
 @Bean Queue guardRegistrationQueue(){return new Queue("guard.visitor-registered",true);}
 @Bean Binding guardRegistrationBinding(Queue guardRegistrationQueue,TopicExchange guardVisitorExchange){return BindingBuilder.bind(guardRegistrationQueue).to(guardVisitorExchange).with("visitor.registered");}
 @Bean Queue guardOaQueue(){return new Queue("guard.oa-approval-updated",true);}
 @Bean Binding guardOaBinding(Queue guardOaQueue,TopicExchange guardVisitorExchange){return BindingBuilder.bind(guardOaQueue).to(guardVisitorExchange).with("oa.approval.updated");}
}
