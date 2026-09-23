package com.wansheng.visitor.dormitory;

import static com.wansheng.visitor.dormitory.EmployeeDormitoryModels.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class EmployeeDormitoryService {
 private final EmployeeDormitoryRepository repo;
 EmployeeDormitoryService(EmployeeDormitoryRepository repo){this.repo=repo;}

 ResourceTree tree(){List<Room> rooms=repo.rooms();List<BuildingNode> nodes=repo.buildings().stream().map(b->new BuildingNode(b,rooms.stream().filter(r->r.buildingId().equals(b.id())).map(r->repo.room(r.id()).orElseThrow()).toList())).toList();return new ResourceTree(nodes);}
 @Transactional Building addBuilding(BuildingCommand c,String op){long id=repo.addBuilding(c);Building b=repo.building(id).orElseThrow();repo.resourceAudit("BUILDING",id,"CREATE",null,b.toString(),op);return b;}
 @Transactional Building updateBuilding(long id,BuildingCommand c,String op){Building old=repo.building(id).orElseThrow(()->bad("楼栋不存在"));if(Boolean.FALSE.equals(c.enabled())&&old.enabled()&&repo.activeInBuilding(id)>0)throw conflict("楼栋存在预订或入住记录，不能停用");found(repo.updateBuilding(id,c));Building updated=repo.building(id).orElseThrow();repo.resourceAudit("BUILDING",id,"UPDATE",old.toString(),updated.toString(),op);return updated;}
 @Transactional Room addRoom(RoomCommand c,String op){enabledBuilding(c.buildingId());long id=repo.addRoom(c);Room room=repo.room(id).orElseThrow();repo.resourceAudit("ROOM",id,"CREATE",null,room.toString(),op);return room;}
 @Transactional Room updateRoom(long id,RoomCommand c,String op){Room old=repo.room(id).orElseThrow(()->bad("房间不存在"));enabledBuilding(c.buildingId());if(repo.activeInRoom(id)>0&&(!old.buildingId().equals(c.buildingId())||Boolean.FALSE.equals(c.enabled())||Boolean.FALSE.equals(c.livable())))throw conflict("房间存在预订或入住记录，不能移动或停用");found(repo.updateRoom(id,c));Room updated=repo.room(id).orElseThrow();repo.resourceAudit("ROOM",id,"UPDATE",old.toString(),updated.toString(),op);return updated;}
 @Transactional Bed addBed(BedCommand c,String op){enabledRoom(c.roomId());long id=repo.addBed(c);Bed bed=repo.bed(id).orElseThrow();repo.resourceAudit("BED",id,"CREATE",null,bed.toString(),op);return bed;}
 @Transactional Bed updateBed(long id,BedCommand c,String op){Bed old=repo.bed(id).orElseThrow(()->bad("床位不存在"));enabledRoom(c.roomId());if(repo.activeOnBed(id)>0&&(!old.roomId().equals(c.roomId())||Boolean.FALSE.equals(c.enabled())))throw conflict("床位存在预订或入住记录，不能移动或停用");found(repo.updateBed(id,c));Bed updated=repo.bed(id).orElseThrow();repo.resourceAudit("BED",id,"UPDATE",old.toString(),updated.toString(),op);return updated;}
 @Transactional Bed setBedCleaning(long id,CleaningCommand c,String op){Bed old=repo.bed(id).orElseThrow(()->bad("床位不存在"));found(repo.setBedCleaning(id,c.required()));Bed updated=repo.bed(id).orElseThrow();repo.resourceAudit("BED",id,"CLEANING",old.toString(),updated.toString(),op);return updated;}
 List<ResourceAudit> resourceAudits(){return repo.resourceAudits();}
 List<StayAudit> stayAudits(){return repo.stayAudits();}
 List<MeterReading> meterReadings(String month){if(month==null||!month.matches("\\d{4}-(0[1-9]|1[0-2])"))throw bad("月份格式应为 YYYY-MM");return repo.meterReadings(month);}
 @Transactional List<MeterReading> saveMeterReadings(List<MeterReadingCommand> commands,String op){if(commands==null||commands.isEmpty())throw bad("请提供抄表数据");String month=commands.get(0).readingMonth();if(commands.stream().anyMatch(c->!month.equals(c.readingMonth())))throw bad("一次只能保存同一月份的抄表数据");for(MeterReadingCommand c:commands){enabledRoom(c.roomId());repo.saveMeterReading(c,op);}return repo.meterReadings(month);}
 List<Person> people(String name){return repo.people(name);} Person addPerson(PersonCommand c){PersonCommand safe=personDefaults(c);return repo.person(repo.addPerson(safe)).orElseThrow();} Person updatePerson(long id,PersonCommand c){found(repo.updatePerson(id,personDefaults(c)));return repo.person(id).orElseThrow();} List<Stay> personStays(long id){repo.person(id).orElseThrow(()->bad("人员不存在"));return repo.staysByPerson(id);}
 @Transactional void deletePerson(long id,String op){Person p=repo.person(id).orElseThrow(()->bad("人员不存在"));if(repo.stayCountForPerson(id)>0)throw conflict("该人员存在住宿记录，请先删除全部住宿记录");repo.resourceAudit("PERSON",id,"DELETE",p.toString(),null,op);found(repo.deletePerson(id));}
 @Transactional ImportSummary importPeople(List<PersonCommand> commands,String op){if(commands==null||commands.isEmpty())throw bad("导入文件没有人员数据");int created=0;List<String> skipped=new ArrayList<>();for(int i=0;i<commands.size();i++){PersonCommand c=commands.get(i);if(repo.person(c.name().trim(),c.department().trim()).isPresent()){skipped.add("第"+(i+2)+"行："+c.name()+"/"+c.department()+"已存在");continue;}repo.addPerson(c);created++;}return new ImportSummary(commands.size(),created,0,0,0,skipped);}
 @Transactional ImportSummary importResources(List<ResourceImportCommand> commands,String op){if(commands==null||commands.isEmpty())throw bad("导入文件没有房间床位数据");int buildings=0,rooms=0,beds=0;List<String> skipped=new ArrayList<>();for(int i=0;i<commands.size();i++){ResourceImportCommand c=commands.get(i);Building building=repo.building(c.buildingName().trim(),c.regionName().trim()).orElse(null);if(building==null){building=addBuilding(new BuildingCommand(c.buildingName().trim(),c.regionName().trim(),true,0),op);buildings++;}Room room=repo.room(building.id(),c.roomNo().trim()).orElse(null);if(room==null){room=addRoom(new RoomCommand(building.id(),c.roomNo().trim(),c.floorNo(),c.facing(),c.roomType().trim(),true,false,null,null,null,null,0,null,true),op);rooms++;}if(repo.bed(c.bedCode().trim()).isPresent()){skipped.add("第"+(i+2)+"行：床位编码"+c.bedCode()+"已存在");continue;}addBed(new BedCommand(room.id(),c.bedLabel().trim(),c.bedCode().trim(),c.threePiece(),true),op);beds++;}return new ImportSummary(commands.size(),0,buildings,rooms,beds,skipped);}
 @Transactional StayImportSummary importStays(List<StayImportCommand> commands,String op){
  if(commands==null||commands.isEmpty())throw bad("导入文件没有入住数据");
  int staysCreated=0,staysUpdated=0,peopleCreated=0;
  List<String> skipped=new ArrayList<>();
  for(int i=0;i<commands.size();i++){
   StayImportCommand c=commands.get(i);
   if(blank(c.bedCode())){skipped.add("第"+(i+2)+"行：床位编码为空，未创建住宿记录");continue;}
   try{
    Bed bed=repo.bed(c.bedCode().trim()).orElseThrow(()->bad("床位编码不存在"));
    Room room=repo.room(bed.roomId()).orElseThrow(()->bad("床位所属房间不存在"));
    Building building=repo.building(room.buildingId()).orElseThrow(()->bad("床位所属楼栋不存在"));
    if(!blank(c.buildingName())&&!building.name().equals(c.buildingName().trim()))throw bad("楼栋名称与床位编码不匹配");
    if(!blank(c.roomNo())&&!room.roomNo().equals(c.roomNo().trim()))throw bad("房号与床位编码不匹配");
    String name=blank(c.name())?"未填写":c.name().trim();
    String department=blank(c.department())?"未填写":c.department().trim();
    String gender=blank(c.gender())?"未填写":c.gender().trim();
    String category=normalizeCategory(c.category());
    String bedType=blank(c.bedType())?room.roomType():c.bedType().trim();
    LocalDate plannedMoveIn=c.plannedMoveIn()==null?LocalDate.now(ZoneId.of("Asia/Shanghai")):c.plannedMoveIn();
    Optional<Stay> existing=repo.activeStayForImport(bed.id(),name,department,plannedMoveIn,c.plannedMoveOut());
    if(existing.isPresent()){
     updateStay(existing.get().id(),new UpdateStayCommand(name,c.centerName(),department,gender,category,c.positionName(),c.rankName(),c.applicationCode(),c.liaison(),bedType,c.threePiece(),c.threePieceNote(),Boolean.TRUE.equals(c.costCut()),c.promiseSigned(),c.cleaningRequired(),c.moveInWater(),c.moveInElectric(),c.moveOutWater(),c.moveOutElectric(),plannedMoveIn,c.plannedMoveOut(),c.specialNote(),c.remark()),op);
     staysUpdated++;
     continue;
    }
    Person person=repo.person(name,department).orElse(null);
    if(person==null){long personId=repo.addPerson(new PersonCommand(name,c.centerName(),department,gender,category,c.positionName(),c.rankName()));person=repo.person(personId).orElseThrow();peopleCreated++;}
    book(new BookCommand(person.id(),bed.id(),c.applicationCode(),c.liaison(),bedType,c.threePiece(),c.threePieceNote(),Boolean.TRUE.equals(c.costCut()),c.promiseSigned(),c.cleaningRequired(),c.moveInWater(),c.moveInElectric(),c.moveOutWater(),c.moveOutElectric(),plannedMoveIn,c.plannedMoveOut(),c.specialNote(),c.remark()),op);
    staysCreated++;
   }catch(ResponseStatusException e){skipped.add("第"+(i+2)+"行："+e.getReason());}
  }
  return new StayImportSummary(commands.size(),staysCreated,staysUpdated,peopleCreated,skipped);
 }
 DormitoryStatistics statistics(){ResourceTree tree=tree();List<Stay> all=repo.stays(null,null,null);List<Person> persons=repo.people(null);List<BuildingNode> enabledBuildings=tree.buildings().stream().filter(n->n.building().enabled()).toList();List<Room> enabledRooms=enabledBuildings.stream().flatMap(n->n.rooms().stream()).filter(r->r.enabled()&&r.livable()).toList();Set<Long> enabledBedIds=enabledRooms.stream().flatMap(r->r.beds().stream()).filter(Bed::enabled).map(Bed::id).collect(java.util.stream.Collectors.toSet());int rooms=enabledRooms.size(),beds=enabledBedIds.size();int booked=(int)all.stream().filter(s->enabledBedIds.contains(s.bed().id())&&s.status()==StayStatus.BOOKED).count(),checkedIn=(int)all.stream().filter(s->enabledBedIds.contains(s.bed().id())&&s.status()==StayStatus.CHECKED_IN).count(),checkedOut=count(all,StayStatus.CHECKED_OUT),cancelled=count(all,StayStatus.CANCELLED);List<StatisticsItem> byBuilding=enabledBuildings.stream().map(n->{Set<Long> ids=n.rooms().stream().filter(r->r.enabled()&&r.livable()).flatMap(r->r.beds().stream()).filter(Bed::enabled).map(Bed::id).collect(java.util.stream.Collectors.toSet());int active=(int)all.stream().filter(s->ids.contains(s.bed().id())&&(s.status()==StayStatus.BOOKED||s.status()==StayStatus.CHECKED_IN)).count();return new StatisticsItem(n.building().name(),ids.size(),active);}).toList();List<StatisticsItem> categories=persons.stream().collect(java.util.stream.Collectors.groupingBy(Person::category,java.util.stream.Collectors.counting())).entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->new StatisticsItem(e.getKey(),e.getValue().intValue(),(int)all.stream().filter(s->enabledBedIds.contains(s.bed().id())&&s.person().category().equals(e.getKey())&&(s.status()==StayStatus.BOOKED||s.status()==StayStatus.CHECKED_IN)).count())).toList();List<StatisticsItem> statuses=List.of(new StatisticsItem("已预定",booked,booked),new StatisticsItem("已入住",checkedIn,checkedIn),new StatisticsItem("已退宿",checkedOut,0),new StatisticsItem("已取消",cancelled,0));return new DormitoryStatistics(new StatisticsSummary(persons.size(),enabledBuildings.size(),rooms,beds,booked,checkedIn,checkedOut,cancelled,Math.max(0,beds-booked-checkedIn)),byBuilding,categories,statuses);}
 List<Stay> stays(String status,Long buildingId,String name){if(status!=null&&!status.isBlank())try{StayStatus.valueOf(status);}catch(IllegalArgumentException e){throw bad("未知住宿状态");}return repo.stays(status,buildingId,name);}
 Stay stay(long id){return repo.stay(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"住宿记录不存在"));}

 @Transactional Stay book(BookCommand c,String op){BookCommand safe=bookDefaults(c);repo.person(safe.personId()).orElseThrow(()->bad("人员不存在"));Bed b=enabledBed(safe.bedId());validateDates(safe.plannedMoveIn(),safe.plannedMoveOut());ensureAvailable(safe.bedId(),safe.personId(),-1,safe.plannedMoveIn(),safe.plannedMoveOut());long id=repo.book(safe,op);if(Boolean.TRUE.equals(safe.cleaningRequired()))repo.setBedCleaning(safe.bedId(),true);repo.audit(id,"BOOK",null,b.id(),null,"BOOKED",op,null);return stay(id);}
 @Transactional Stay updateStay(long id,UpdateStayCommand c,String op){Stay s=stay(id);UpdateStayCommand safe=stayDefaults(c);validateDates(safe.plannedMoveIn(),safe.plannedMoveOut());found(repo.updatePerson(s.person().id(),personDefaults(new PersonCommand(safe.name(),safe.centerName(),safe.department(),safe.gender(),safe.category(),safe.positionName(),safe.rankName()))));ensureAvailable(s.bed().id(),s.person().id(),id,safe.plannedMoveIn(),safe.plannedMoveOut());if(repo.updateStay(id,safe,op)!=1)throw conflict("只有预订或入住中的记录可以编辑");repo.setBedCleaning(s.bed().id(),Boolean.TRUE.equals(safe.cleaningRequired()));repo.audit(id,"UPDATE",s.bed().id(),s.bed().id(),s.status().name(),s.status().name(),op,"编辑入住信息");return stay(id);}
 @Transactional Stay checkIn(long id,String op){Stay s=stay(id);repo.lockBed(s.bed().id());if(repo.checkedInByAnotherStay(s.bed().id(),id))throw conflict("床位仍有未退宿人员，暂不能入住");if(repo.transition(id,"BOOKED","CHECKED_IN",op)!=1)throw conflict("只有已预订记录可以入住");repo.audit(id,"CHECK_IN",s.bed().id(),s.bed().id(),"BOOKED","CHECKED_IN",op,null);return stay(id);}
 @Transactional Stay transfer(long id,TransferCommand c,String op){Stay s=stay(id);enabledBed(c.bedId());ensureAvailable(c.bedId(),s.person().id(),id,s.plannedMoveIn(),s.plannedMoveOut());if(repo.transfer(id,c.bedId(),op)!=1)throw conflict("只有预订或入住中的记录可以调宿");repo.audit(id,"TRANSFER",s.bed().id(),c.bedId(),s.status().name(),s.status().name(),op,c.reason());return stay(id);}
 @Transactional Stay extend(long id,ExtendCommand c,String op){Stay s=stay(id);if(c.plannedMoveOut().isBefore(s.plannedMoveIn()))throw bad("计划退宿日期不得早于入住日期");ensureAvailable(s.bed().id(),s.person().id(),id,s.plannedMoveIn(),c.plannedMoveOut());if(repo.extend(id,c.plannedMoveOut(),op)!=1)throw conflict("只有预订或入住中的记录可以续住");repo.audit(id,"EXTEND",s.bed().id(),s.bed().id(),s.status().name(),s.status().name(),op,c.reason());return stay(id);}
 @Transactional Stay checkout(long id,CheckoutCommand c,String op){Stay s=stay(id);if(repo.checkout(id,c,op)!=1)throw conflict("只有已入住记录可以退宿");repo.audit(id,"CHECK_OUT",s.bed().id(),null,"CHECKED_IN","CHECKED_OUT",op,c.reason());return stay(id);}
 @Transactional Stay cancel(long id,String op){Stay s=stay(id);if(repo.transition(id,"BOOKED","CANCELLED",op)!=1)throw conflict("只有已预订记录可以取消");repo.audit(id,"CANCEL",s.bed().id(),null,"BOOKED","CANCELLED",op,null);return stay(id);}
 @Transactional void deleteTerminalStay(long id,String op){Stay s=stay(id);if(s.status()!=StayStatus.CANCELLED&&s.status()!=StayStatus.CHECKED_OUT)throw conflict("只有已取消或已退宿记录可以删除");repo.resourceAudit("STAY",id,"DELETE",s.toString(),null,op);repo.deleteStayAudits(id);found(repo.deleteStay(id));}

 private void ensureAvailable(long bedId,long personId,long except,java.time.LocalDate plannedMoveIn,java.time.LocalDate plannedMoveOut){enabledBed(bedId);repo.lockBed(bedId);if(repo.occupied(bedId,except,plannedMoveIn,plannedMoveOut))throw conflict("该床位在所选入住日期内存在冲突");if(repo.mixedGender(bedId,personId,except,plannedMoveIn,plannedMoveOut))throw conflict("所选日期内标间禁止男女混住");}
 private Building enabledBuilding(long id){Building b=repo.building(id).orElseThrow(()->bad("楼栋不存在"));if(!b.enabled())throw conflict("楼栋已停用");return b;}
 private Room enabledRoom(long id){Room r=repo.room(id).orElseThrow(()->bad("房间不存在"));if(!r.enabled()||!r.livable())throw conflict("房间不可入住或已停用");return r;}
 private Bed enabledBed(long id){Bed b=repo.bed(id).orElseThrow(()->bad("床位不存在"));enabledRoom(b.roomId());if(!b.enabled())throw conflict("床位已停用");return b;}
 private static void validateDates(java.time.LocalDate in,java.time.LocalDate out){if(out!=null&&out.isBefore(in))throw bad("计划退宿日期不得早于入住日期");}
 private static boolean blank(String value){return value==null||value.isBlank();}
 private static String normalizeCategory(String value){if(blank(value))return "未分类";String normalized=value.trim().replace('（','(').replace('）',')');return switch(normalized){case "已审批长住人","已审批长住人员","已审批长住员工"->"己审批长住员工";default->normalized;};}
 private static PersonCommand personDefaults(PersonCommand c){return new PersonCommand(blank(c.name())?"未填写":c.name().trim(),c.centerName(),blank(c.department())?"未填写":c.department().trim(),blank(c.gender())?"未填写":c.gender().trim(),blank(c.category())?"未分类":c.category().trim(),c.positionName(),c.rankName());}
 private static BookCommand bookDefaults(BookCommand c){return new BookCommand(c.personId(),c.bedId(),c.applicationCode(),c.liaison(),blank(c.bedType())?"未填写":c.bedType().trim(),c.threePiece(),c.threePieceNote(),Boolean.TRUE.equals(c.costCut()),c.promiseSigned(),Boolean.TRUE.equals(c.cleaningRequired()),c.moveInWater(),c.moveInElectric(),c.moveOutWater(),c.moveOutElectric(),c.plannedMoveIn()==null?LocalDate.now(ZoneId.of("Asia/Shanghai")):c.plannedMoveIn(),c.plannedMoveOut(),c.specialNote(),c.remark());}
 private static UpdateStayCommand stayDefaults(UpdateStayCommand c){return new UpdateStayCommand(blank(c.name())?"未填写":c.name().trim(),c.centerName(),blank(c.department())?"未填写":c.department().trim(),blank(c.gender())?"未填写":c.gender().trim(),blank(c.category())?"未分类":c.category().trim(),c.positionName(),c.rankName(),c.applicationCode(),c.liaison(),blank(c.bedType())?"未填写":c.bedType().trim(),c.threePiece(),c.threePieceNote(),Boolean.TRUE.equals(c.costCut()),c.promiseSigned(),Boolean.TRUE.equals(c.cleaningRequired()),c.moveInWater(),c.moveInElectric(),c.moveOutWater(),c.moveOutElectric(),c.plannedMoveIn()==null?LocalDate.now(ZoneId.of("Asia/Shanghai")):c.plannedMoveIn(),c.plannedMoveOut(),c.specialNote(),c.remark());}
 private static int count(List<Stay> stays,StayStatus status){return(int)stays.stream().filter(s->s.status()==status).count();}
 private static void found(int n){if(n!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 private static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private static ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
