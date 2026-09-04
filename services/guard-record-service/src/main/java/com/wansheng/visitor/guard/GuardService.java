package com.wansheng.visitor.guard;
import java.time.Clock; import java.util.List; import java.util.Locale; import java.util.UUID;
import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException;
@Service
class GuardService {
  private final GuardRepository repo; private final DormitoryCancellationPolicy cancellation; private final Clock clock=Clock.systemUTC();
  GuardService(GuardRepository repo,DormitoryCancellationPolicy cancellation){this.repo=repo;this.cancellation=cancellation;}
  List<GuardRecord> list(GuardStatus s){return repo.list(s);} GuardRecord find(String id){return repo.find(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"门卫记录不存在"));}
  @Transactional GuardRecord create(GuardRecordRequest request,String op){validateVehicle(request);String id="GUARD-"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);GuardRecord record=new GuardRecord(id,request.visitorName().trim(),request.mobile().trim(),request.hostName().trim(),normalize(request.plateNumber()),request.vehicleEnteringFactory(),request.accommodationRequired(),"NOT_STARTED",GuardStatus.WAITING_ENTRY,null,null,null,null);repo.createManual(record,op,clock.instant());return find(id);}
  @Transactional GuardRecord update(String id,GuardRecordRequest request,String op){validateVehicle(request);find(id);if(!request.accommodationRequired())cancellation.check(id);if(repo.updateDetails(id,request,op,clock.instant())!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"门卫记录不存在");return find(id);}
  @Transactional GuardRecord entry(String id,String op){transition(id,GuardStatus.WAITING_ENTRY,GuardStatus.IN_FACTORY,op);return find(id);}
  @Transactional GuardRecord exit(String id,String op){transition(id,GuardStatus.IN_FACTORY,GuardStatus.EXITED,op);return find(id);}
  private void transition(String id,GuardStatus f,GuardStatus t,String op){if(repo.transition(id,f,t,op,clock.instant())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"当前状态不允许该操作");}
  private static void validateVehicle(GuardRecordRequest request){if(Boolean.TRUE.equals(request.vehicleEnteringFactory())&&(request.plateNumber()==null||request.plateNumber().isBlank()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"车辆进厂时必须填写车牌号");}
  private static String normalize(String value){return value==null||value.isBlank()?null:value.trim();}
}
