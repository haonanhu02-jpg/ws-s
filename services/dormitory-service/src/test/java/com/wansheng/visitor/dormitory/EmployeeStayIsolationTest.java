package com.wansheng.visitor.dormitory;

import static com.wansheng.visitor.dormitory.EmployeeDormitoryModels.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

class EmployeeStayIsolationTest {
 private EmployeeDormitoryRepository repo;
 private EmployeeDormitoryService service;
 private DormitoryExtensionRepository extension;
 private Stay room309,room313;
 private static final LocalDate IN=LocalDate.of(2026,9,1);
 @BeforeEach void setup(){
  JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:stay_isolation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
  Flyway.configure().dataSource(ds).cleanDisabled(false).load().clean();
  Flyway.configure().dataSource(ds).load().migrate();
  repo=new EmployeeDormitoryRepository(new JdbcTemplate(ds));service=new EmployeeDormitoryService(repo);
  extension=new DormitoryExtensionRepository(new JdbcTemplate(ds));
  long building=repo.addBuilding(new BuildingCommand("测试公寓","测试",true,0));
  long person=repo.addPerson(new PersonCommand("欧阳春","工程中心","工程管理部","男","己审批长住员工","工程师","T4"));
  room309=book(person,building,"309");room313=book(person,building,"313");
 }
 private Stay book(long person,long building,String no){
  long room=repo.addRoom(new RoomCommand(building,no,3,"南","单间",true,false,null,null,null,null,0,null,true));
  long bed=repo.addBed(new BedCommand(room,"单床","测试公寓-"+no+"-单床",null,true));
  return service.book(new BookCommand(person,bed,null,null,"长住房",null,null,false,null,false,null,null,null,null,IN,null,null,null),"test");
 }
 private UpdateStayCommand details(String name){return details(name,null);}
 private UpdateStayCommand details(String name,LocalDate end){return new UpdateStayCommand(name,"新中心","新部门","男","己审批长住员工","新岗位","新职级","APP-309",null,"长住房",null,null,false,null,false,null,null,null,null,IN,end,null,null);}
 @ParameterizedTest @ValueSource(strings={"盛心公寓","伏龙宿舍","花城宿舍","岙底罗"})
 void explicitCheckInAlsoConfirmsOnlyTheSelectedArrival(String name){
  long building=repo.addBuilding(new BuildingCommand(name+"手工入住测试","测试",true,0));
  Stay current=book(room309.person().id(),building,"201");
  service.extend(current.id(),new ExtendCommand(IN.plusDays(10),null),"test");current=service.stay(current.id());
  Stay next=service.book(new BookCommand(current.person().id(),current.bed().id(),"NEXT",null,"长住房",null,null,false,null,false,null,null,null,null,IN.plusDays(11),null,null,null),"test");
  Stay checked=new EmployeeDormitoryController(service,null).checkIn(current.id(),()->"operator");
  assertThat(checked.status()).isEqualTo(StayStatus.CHECKED_IN);assertThat(checked.arrivalConfirmed()).isTrue();assertThat(checked.checkedInAt()).isNotNull();
  assertThat(service.stay(checked.id())).isEqualTo(checked);
  assertThat(checked).usingRecursiveComparison().ignoringFields("status","checkedInAt","version","arrivalConfirmed").isEqualTo(current);
  assertThat(service.stay(next.id())).isEqualTo(next);assertThat(service.stay(room309.id())).isEqualTo(room309);assertThat(service.stay(room313.id())).isEqualTo(room313);
  assertThat(service.stayAudits().stream().filter(a->a.stayId().equals(checked.id())).map(StayAudit::action)).contains("CHECK_IN","ARRIVAL_CONFIRM");
 }
 @Test void importedCheckInDoesNotInventManualArrivalConfirmation(){
  service.cancel(room313.id(),"test");Stay previous=service.stay(room313.id());
  StayImportCommand row=new StayImportCommand("导入人员","中心","部门","男","己审批长住员工","岗位","T5","测试公寓","313",room313.bed().bedCode(),"IMPORT",null,"长住房",null,null,false,null,false,null,null,null,null,IN,IN.plusDays(10),null,null,"CHECKED_IN");
  assertThat(service.importStays(java.util.List.of(row),"importer").staysCreated()).isEqualTo(1);
  Stay imported=service.stays("CHECKED_IN",null,"导入人员").stream().findFirst().orElseThrow();
  assertThat(imported.arrivalConfirmed()).isFalse();assertThat(imported.checkedInAt()).isNotNull();assertThat(imported.plannedMoveIn()).isEqualTo(IN);assertThat(imported.plannedMoveOut()).isEqualTo(IN.plusDays(10));
  assertThat(service.stay(room309.id())).isEqualTo(room309);assertThat(service.stay(previous.id())).isEqualTo(previous);
  assertThat(service.stayAudits().stream().filter(a->a.stayId().equals(imported.id())).map(StayAudit::action)).contains("CHECK_IN").doesNotContain("ARRIVAL_CONFIRM");
 }
 @ParameterizedTest @ValueSource(strings={"盛心公寓","伏龙宿舍","花城宿舍","岙底罗"})
 void arrivalConfirmationIsIndependentAndPersistsPerStay(String name){
  long building=repo.addBuilding(new BuildingCommand(name+"确认测试","测试",true,0));
  Stay current=book(room309.person().id(),building,"201");
  service.extend(current.id(),new ExtendCommand(IN.plusDays(10),null),"test");current=service.stay(current.id());
  Stay next=service.book(new BookCommand(current.person().id(),current.bed().id(),"NEXT",null,"长住房",null,null,false,null,false,null,null,null,null,IN.plusDays(11),null,null,null),"test");
  assertThat(current.arrivalConfirmed()).isFalse();assertThat(next.arrivalConfirmed()).isFalse();
  Stay checked=service.setArrivalConfirmation(current.id(),new ArrivalConfirmationCommand(true,current.version()),"operator");
  assertThat(checked.arrivalConfirmed()).isTrue();assertThat(repo.stay(current.id()).orElseThrow().arrivalConfirmed()).isTrue();
  assertThat(checked).usingRecursiveComparison().ignoringFields("version","arrivalConfirmed").isEqualTo(current);
  assertThat(service.stay(next.id())).isEqualTo(next);assertThat(service.stay(room313.id())).isEqualTo(room313);
  assertThatThrownBy(()->service.setArrivalConfirmation(checked.id(),new ArrivalConfirmationCommand(false,checked.version()-1),"test")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThat(service.stay(checked.id()).arrivalConfirmed()).isTrue();
  Stay edited=service.updateStay(checked.id(),details("只改此人",checked.plannedMoveOut()),"test");
  assertThat(edited.arrivalConfirmed()).isTrue();
  Stay unchecked=service.setArrivalConfirmation(edited.id(),new ArrivalConfirmationCommand(false,edited.version()),"operator");
  assertThat(unchecked.arrivalConfirmed()).isFalse();assertThat(unchecked.status()).isEqualTo(StayStatus.BOOKED);assertThat(unchecked.checkedInAt()).isNull();
  assertThat(service.stayAudits().stream().filter(a->a.stayId().equals(unchecked.id())).map(StayAudit::action)).contains("ARRIVAL_CONFIRM","ARRIVAL_CLEAR");
 }
 @Test void arrivalConfirmationRejectsEndedRecordsWithoutChangingOtherDetails(){
  Stay ended=service.cancel(room309.id(),"test");
  assertThatThrownBy(()->service.setArrivalConfirmation(ended.id(),new ArrivalConfirmationCommand(true,ended.version()),"test")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThat(service.stay(ended.id())).isEqualTo(ended);
 }
 @ParameterizedTest @ValueSource(strings={"盛心公寓","伏龙宿舍","花城宿舍","岙底罗"})
 void preparingCleaningEndsOnlySelectedStayAndPreservesHistory(String name){
  long building=repo.addBuilding(new BuildingCommand(name+"清洁测试","测试",true,0));
  Stay current=book(room309.person().id(),building,"201");
  service.extend(current.id(),new ExtendCommand(IN.plusDays(10),null),"test");
  current=service.stay(current.id());
  Stay next=service.book(new BookCommand(current.person().id(),current.bed().id(),"NEXT",null,"长住房",null,null,false,null,false,null,null,null,null,IN.plusDays(11),null,null,null),"test");
  Bed bed=service.prepareCleaning(current.bed().id(),new PrepareCleaningCommand(current.id(),current.version(),IN.plusDays(5)),"cleaner");
  Stay ended=service.stay(current.id());
  assertThat(bed.cleaningRequired()).isTrue();assertThat(ended.status()).isEqualTo(StayStatus.CHECKED_OUT);
  assertThat(ended.person()).isEqualTo(current.person());assertThat(ended.checkedOutAt()).isNotNull();
  assertThat(service.stay(next.id())).usingRecursiveComparison().ignoringFields("bed.cleaningRequired").isEqualTo(next);assertThat(service.stay(room313.id())).isEqualTo(room313);
  assertThat(service.stayAudits().stream().filter(a->a.stayId().equals(ended.id())).map(StayAudit::action)).contains("CHECK_OUT");
 }
 @Test void futureSavedAsCheckedInIsCancelledForCleaning(){
  Stay future=service.checkIn(room309.id(),"test");
  service.prepareCleaning(future.bed().id(),new PrepareCleaningCommand(future.id(),future.version(),IN.minusDays(1)),"test");
  assertThat(service.stay(future.id()).status()).isEqualTo(StayStatus.CANCELLED);
  assertThat(service.stay(future.id()).person()).isEqualTo(future.person());
 }
 @Test void cleaningRejectsWrongBedAndStaleVersionsBeforeWriting(){
  assertThatThrownBy(()->service.prepareCleaning(room309.bed().id(),new PrepareCleaningCommand(room313.id(),room313.version(),IN),"test")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThatThrownBy(()->service.prepareCleaning(room309.bed().id(),new PrepareCleaningCommand(room309.id(),room309.version()+1,IN),"test")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  assertThat(repo.bed(room309.bed().id()).orElseThrow().cleaningRequired()).isFalse();
  assertThat(service.stay(room309.id())).isEqualTo(room309);
 }
 @Test void emptyCleaningRequestDoesNotCancelAnyReservation(){
  service.prepareCleaning(room309.bed().id(),new PrepareCleaningCommand(null,null,IN),"test");
  assertThat(service.stay(room309.id())).usingRecursiveComparison().ignoringFields("bed.cleaningRequired").isEqualTo(room309);
  assertThat(repo.bed(room309.bed().id()).orElseThrow().cleaningRequired()).isTrue();
 }
 @Test void clearing313AndReentering309DoesNotCopyAcrossRooms(){
  service.updateStay(room313.id(),details(""),"test");
  assertThat(service.stay(room309.id()).person().name()).isEqualTo("欧阳春");
  service.updateStay(room309.id(),details("重新录入"),"test");
  assertThat(service.stay(room313.id()).person().name()).isEmpty();
  assertThat(service.stay(room309.id()).person().name()).isEqualTo("重新录入");
  assertThat(service.stay(room309.id()).person().id()).isEqualTo(room309.person().id());
  assertThat(extension.occupantNames(room309.bed().roomId(),IN,IN.plusMonths(1))).containsExactly("重新录入");
  assertThat(extension.settlementStays(IN,IN.plusMonths(1)).stream().filter(s -> s.stayId()==room309.id()).findFirst().orElseThrow().personName()).isEqualTo("重新录入");
 }
 @Test void cancellingAndDeleting313Leaves309Intact(){
  service.cancel(room313.id(),"test");service.deleteTerminalStay(room313.id(),"test");
  assertThat(service.stay(room309.id())).isEqualTo(room309);
  assertThat(repo.stay(room313.id())).isEmpty();
 }
 @ParameterizedTest
 @ValueSource(strings={"盛心公寓","伏龙宿舍","花城宿舍","岙底罗"})
 void isolatesEveryDormitoryAndAllResidentFields(String name){
  long building=repo.buildings().stream().filter(b -> b.name().equals(name)).map(Building::id).findFirst()
   .orElseGet(() -> repo.addBuilding(new BuildingCommand(name,"测试",true,0)));
  Stay other=book(room309.person().id(),building,"隔离测试201");
  service.updateStay(other.id(),details("其他房间"),"test");
  assertThat(service.stay(room309.id())).isEqualTo(room309);
  assertThat(service.stay(room313.id())).isEqualTo(room313);
  assertThat(service.stay(other.id()).person()).extracting(Person::name,Person::centerName,Person::department,Person::positionName,Person::rankName)
   .containsExactly("其他房间","新中心","新部门","新岗位","新职级");
  assertThat(service.stays(null,building,"其他房间")).extracting(Stay::id).containsExactly(other.id());
  assertThat(service.personStays(room309.person().id())).hasSize(3);
 }
 @Test void sameBedConsecutiveReservationsAndHistoryAreIndependent(){
  service.extend(room309.id(),new ExtendCommand(LocalDate.of(2026,9,15),null),"test");
  Stay first=service.stay(room309.id());
  Stay next=service.book(new BookCommand(first.person().id(),first.bed().id(),null,null,"长住房",null,null,false,null,false,null,null,null,null,LocalDate.of(2026,9,16),null,null,null),"test");
  service.updateStay(next.id(),new UpdateStayCommand("后续预订","新中心","新部门","男","己审批长住员工","新岗位","新职级",null,null,"长住房",null,null,false,null,false,null,null,null,null,LocalDate.of(2026,9,16),null,null,null),"test");
  assertThat(service.stay(first.id())).isEqualTo(first);
  service.checkIn(first.id(),"test");service.checkout(first.id(),new CheckoutCommand(null,null,null),"test");
  Stay history=service.stay(first.id());
  service.updateStay(room313.id(),details("另外的名字"),"test");
  assertThat(service.stay(first.id())).isEqualTo(history);
 }
 @Test void importingOneRoomAndEditingMasterProfileDoNotRewriteOtherStays(){
  StayImportCommand row=new StayImportCommand("欧阳春","导入中心","工程管理部","男","己审批长住员工","导入岗位","T5","测试公寓","309",room309.bed().bedCode(),"APP-309",null,"长住房",null,null,false,null,false,null,null,null,null,IN,null,null,null,"BOOKED");
  assertThat(service.importStays(java.util.List.of(row),"test").staysUpdated()).isEqualTo(1);
  assertThat(service.stay(room309.id()).person().centerName()).isEqualTo("导入中心");
  assertThat(service.stay(room313.id())).isEqualTo(room313);
  service.updatePerson(room309.person().id(),new PersonCommand("档案姓名","档案中心","档案部门","女","档案类别",null,null));
  assertThat(service.stay(room309.id()).person().name()).isEqualTo("欧阳春");
  assertThat(service.stay(room313.id())).isEqualTo(room313);
 }
 @Test void newImportedStayKeepsItsOwnDetailsWhenReusingPerson(){
  service.cancel(room313.id(),"test");
  Stay history=service.stay(room313.id());
  StayImportCommand row=new StayImportCommand("欧阳春","独立中心","工程管理部","男","己审批长住员工","独立岗位","T5","测试公寓","313",room313.bed().bedCode(),"APP-313",null,"长住房",null,null,false,null,false,null,null,null,null,IN,null,null,null,"BOOKED");
  assertThat(service.importStays(java.util.List.of(row),"test").staysCreated()).isEqualTo(1);
  assertThat(service.stay(room309.id())).isEqualTo(room309);
  assertThat(service.stay(room313.id())).isEqualTo(history);
  assertThat(service.personStays(room309.person().id()).stream().filter(s -> !s.id().equals(room309.id()) && !s.id().equals(room313.id())).findFirst().orElseThrow().person().centerName()).isEqualTo("独立中心");
 }
 @ParameterizedTest @ValueSource(strings={"盛心公寓","伏龙宿舍","花城宿舍","岙底罗"})
 void importedStayCanBeEditedAndQueriedWithModifiedDetails(String name){
  long building=repo.addBuilding(new BuildingCommand(name+"导入编辑","测试",true,0));
  long room=repo.addRoom(new RoomCommand(building,"101",1,"南","单间",true,false,null,null,null,null,0,null,true));
  String code=name+"导入编辑-101";
  repo.addBed(new BedCommand(room,"单床",code,null,true));
  StayImportCommand row=new StayImportCommand("导入姓名","导入中心","导入部门",null,null,null,null,name+"导入编辑","101",code,null,null,null,null,null,false,null,false,null,null,null,null,IN,null,null,null,"CHECKED_IN");
  assertThat(service.importStays(java.util.List.of(row),"test").staysCreated()).isEqualTo(1);
  Stay imported=service.stays(null,building,null).get(0);
  UpdateStayCommand command=new UpdateStayCommand("修改姓名","修改中心","修改部门",null,"己审批长住员工","修改岗位","T6","UPDATED",null,"长住房",null,null,false,true,false,null,null,null,null,IN,null,null,"修改备注");
  try(var validator=jakarta.validation.Validation.buildDefaultValidatorFactory()){
   assertThat(validator.getValidator().validate(command)).isEmpty();
  }
  service.updateStay(imported.id(),command,"editor");
  Stay reloaded=service.stays(null,building,"修改姓名").get(0);
  assertThat(reloaded.person()).extracting(Person::name,Person::centerName,Person::department,Person::positionName,Person::rankName).containsExactly("修改姓名","修改中心","修改部门","修改岗位","T6");
  assertThat(reloaded.applicationCode()).isEqualTo("UPDATED");assertThat(reloaded.remark()).isEqualTo("修改备注");
  assertThat(reloaded.id()).isEqualTo(imported.id());assertThat(reloaded.version()).isEqualTo(imported.version()+1);
  assertThat(service.stay(room309.id())).isEqualTo(room309);
 }
 @Test void migrationBackfillsSharedRecordsWithoutChangingIdentityOrDates(){
  JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:stay_upgrade;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
  Flyway.configure().dataSource(ds).cleanDisabled(false).load().clean();
  Flyway.configure().dataSource(ds).target("16").load().migrate();
  JdbcTemplate jdbc=new JdbcTemplate(ds);
  long person=new EmployeeDormitoryRepository(jdbc).addPerson(new PersonCommand("升级姓名",null,"升级部门","男","员工",null,null));
  Long bed=jdbc.queryForObject("SELECT MIN(id) FROM dorm_bed",Long.class);
  jdbc.update("INSERT INTO dorm_stay(person_id,bed_id,status,bed_type,cost_cut,planned_move_in,operator_name) VALUES(?,?,'BOOKED','长住房',FALSE,?,'test')",person,bed,IN);
  Flyway.configure().dataSource(ds).load().migrate();
  jdbc.update("UPDATE dorm_person SET name='之后改的档案' WHERE id=?",person);
  Stay stay=new EmployeeDormitoryRepository(jdbc).stays(null,null,null).get(0);
  assertThat(stay.person().name()).isEqualTo("升级姓名");
  assertThat(stay.person().centerName()).isNull();
  assertThat(stay.person().id()).isEqualTo(person);
  assertThat(stay.plannedMoveIn()).isEqualTo(IN);
 }
}
