package com.wansheng.visitor.dormitory;
import com.fasterxml.jackson.databind.*; import java.time.*; import org.springframework.amqp.core.*; import org.springframework.amqp.rabbit.annotation.RabbitListener; import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.*; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.client.RestClient;
@Component class DormitoryRegistrationConsumer{
 private final ObjectMapper json;private final RestClient client;private final DormitoryRepository repo;private final String url,guardUrl,token;private final DormitoryScopeMode scope;private final Clock clock=Clock.systemUTC();
 DormitoryRegistrationConsumer(ObjectMapper j,RestClient.Builder b,DormitoryRepository r,@Value("${visitor.dormitory.registration-service-url}")String u,@Value("${visitor.dormitory.guard-service-url}")String gu,@Value("${visitor.dormitory.internal-token}")String t,@Value("${visitor.dormitory.scope-mode:ACCOMMODATION_ONLY}")DormitoryScopeMode s){json=j;client=b.build();repo=r;url=u;guardUrl=gu;token=t;scope=s;}
@RabbitListener(queues="dormitory.visitor-registered") @Transactional void receive(String p)throws Exception{sync(p);}
@RabbitListener(queues="dormitory.guard-manual-registered") @Transactional void receiveGuardManual(String p)throws Exception{sync(p);}
private void sync(String p)throws Exception{
 JsonNode e=json.readTree(p);String eid=e.path("eventId").asText(),vid=e.path("visitId").asText();
 if(eid.isBlank()||vid.isBlank())throw new IllegalArgumentException("invalid event");
 // Fetch latest versioned guard snapshot, not stale data from an event payload.
 ManualDormitoryView v=client.get().uri(guardUrl+"/internal/guard/records/{id}/dormitory-view",vid).header("X-Internal-Token",token).retrieve().body(ManualDormitoryView.class);
 if(v==null)throw new IllegalStateException("guard snapshot missing");
 String department="门卫手工登记",reason="门卫手工登记";boolean hasVehicle=v.plateNumber()!=null&&!v.plateNumber().isBlank();
 if(!vid.startsWith("GUARD-")){
  DormitoryView source=client.get().uri(url+"/internal/registrations/{id}/dormitory-view",vid).header("X-Internal-Token",token).retrieve().body(DormitoryView.class);
  if(source==null)throw new IllegalStateException("registration snapshot missing");
  department=source.hostDepartment();reason=source.visitReason();hasVehicle=source.hasVehicle()||hasVehicle;
 }
 boolean rejected=repo.syncGuard(eid,new DormitoryRecord(v.visitId(),v.visitorName(),v.mobile(),v.hostName(),department,reason,v.accommodationRequired(),hasVehicle,v.plateNumber(),v.vehicleEnteringFactory(),false,null),v.detailsVersion(),clock.instant());
 if(rejected)client.post().uri(guardUrl+"/internal/guard/records/{id}/reject-accommodation-cancellation",vid).header("X-Internal-Token",token).body(java.util.Map.of("detailsVersion",v.detailsVersion())).retrieve().toBodilessEntity();
}

record DormitoryView(String visitId,String visitorName,String mobile,String hostName,String hostDepartment,String visitReason,boolean accommodationRequired,boolean hasVehicle,String plateNumber,boolean vehicleEnteringFactory){}
record ManualDormitoryView(String visitId,String visitorName,String mobile,String hostName,boolean accommodationRequired,String plateNumber,boolean vehicleEnteringFactory,long detailsVersion){}
}
@Configuration class DormitoryMessagingConfiguration{
 @Bean TopicExchange dormitoryVisitorExchange(){return new TopicExchange("visitor.events",true,false);}@Bean Queue dormitoryRegistrationQueue(){return new Queue("dormitory.visitor-registered",true);}@Bean Binding dormitoryRegistrationBinding(Queue dormitoryRegistrationQueue,TopicExchange dormitoryVisitorExchange){return BindingBuilder.bind(dormitoryRegistrationQueue).to(dormitoryVisitorExchange).with("visitor.registered");}@Bean Queue dormitoryGuardManualQueue(){return new Queue("dormitory.guard-manual-registered",true);}@Bean Binding dormitoryGuardManualBinding(Queue dormitoryGuardManualQueue,TopicExchange dormitoryVisitorExchange){return BindingBuilder.bind(dormitoryGuardManualQueue).to(dormitoryVisitorExchange).with("guard.manual-visitor-registered");}
}
