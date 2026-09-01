package com.wansheng.visitor.dormitory;
import java.time.*; import java.util.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/visitor/dormitory") class DormitoryController{
 private final DormitoryRepository repo;private final DormitoryScopeMode scope;private final Clock clock=Clock.systemUTC();DormitoryController(DormitoryRepository r,@Value("${visitor.dormitory.scope-mode:UNDECIDED}")DormitoryScopeMode s){repo=r;scope=s;}
 @GetMapping("/records")List<DormitoryRecord>list(){if(scope==DormitoryScopeMode.UNDECIDED)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Q-012 尚未确认");return repo.list(scope==DormitoryScopeMode.ACCOMMODATION_ONLY);}
 @GetMapping("/records/{id}")DormitoryRecord find(@PathVariable String id){return repo.find(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 @PostMapping("/records/{id}/confirm")DormitoryRecord confirm(@PathVariable String id){if(repo.confirm(id)!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"已确认或记录不存在");return find(id);}
 @PostMapping("/records/{id}/assign-bed")DormitoryRecord assign(@PathVariable String id,@RequestBody BedAssignment b,@RequestHeader("X-Operator")String op){if(repo.assign(id,b.bedCode(),op,clock.instant())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"请先确认住宿并检查床位");return find(id);}
 @PostMapping("/records/{id}/change-bed")DormitoryRecord change(@PathVariable String id,@RequestBody BedAssignment b,@RequestHeader("X-Operator")String op){return assign(id,b,op);}
 @PostMapping("/beds")void bed(@RequestBody BedDefinition b){repo.addBed(b.buildingName(),b.roomNumber(),b.bedCode());}
 record BedAssignment(String bedCode){} record BedDefinition(String buildingName,String roomNumber,String bedCode){}
}

