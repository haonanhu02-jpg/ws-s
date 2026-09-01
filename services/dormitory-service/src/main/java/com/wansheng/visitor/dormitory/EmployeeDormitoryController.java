package com.wansheng.visitor.dormitory;

import static com.wansheng.visitor.dormitory.EmployeeDormitoryModels.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/visitor/dormitory/employee")
class EmployeeDormitoryController {
 private final EmployeeDormitoryService service;
 private final DormitoryExtensionService extension;
 EmployeeDormitoryController(EmployeeDormitoryService service,DormitoryExtensionService extension){this.service=service;this.extension=extension;}
 @GetMapping("/resources/tree") ResourceTree tree(){return service.tree();}
 @PostMapping("/buildings") Building addBuilding(@Valid @RequestBody BuildingCommand c,Principal p){return service.addBuilding(c,p.getName());}
 @PutMapping("/buildings/{id}") Building updateBuilding(@PathVariable long id,@Valid @RequestBody BuildingCommand c,Principal p){return service.updateBuilding(id,c,p.getName());}
 @PostMapping("/rooms") Room addRoom(@Valid @RequestBody RoomCommand c,Principal p){return service.addRoom(c,p.getName());}
 @PutMapping("/rooms/{id}") Room updateRoom(@PathVariable long id,@Valid @RequestBody RoomCommand c,Principal p){return service.updateRoom(id,c,p.getName());}
 @PostMapping("/beds") Bed addBed(@Valid @RequestBody BedCommand c,Principal p){return service.addBed(c,p.getName());}
 @PutMapping("/beds/{id}") Bed updateBed(@PathVariable long id,@Valid @RequestBody BedCommand c,Principal p){return service.updateBed(id,c,p.getName());}
 @GetMapping("/resource-audits") List<ResourceAudit> resourceAudits(){return service.resourceAudits();}
 @GetMapping("/stay-audits") List<StayAudit> stayAudits(){return service.stayAudits();}
 @GetMapping("/meter-readings") List<MeterReading> meterReadings(@RequestParam String month){return service.meterReadings(month);}
 @GetMapping("/statistics") DormitoryStatistics statistics(){return service.statistics();}
 @PutMapping("/meter-readings") List<MeterReading> saveMeterReadings(@Valid @RequestBody List<@Valid MeterReadingCommand> commands,Principal p){return service.saveMeterReadings(commands,p.getName());}
 @GetMapping("/people") List<Person> people(@RequestParam(required=false)String name){return service.people(name);}
 @PostMapping("/people") Person addPerson(@Valid @RequestBody PersonCommand c){return service.addPerson(c);}
 @PutMapping("/people/{id}") Person updatePerson(@PathVariable long id,@Valid @RequestBody PersonCommand c){return service.updatePerson(id,c);}
 @PostMapping("/imports/people") ImportSummary importPeople(@Valid @RequestBody List<@Valid PersonCommand> commands,Principal p){return service.importPeople(commands,p.getName());}
 @PostMapping("/imports/resources") ImportSummary importResources(@Valid @RequestBody List<@Valid ResourceImportCommand> commands,Principal p){return service.importResources(commands,p.getName());}
 @GetMapping("/people/{id}/stays") List<Stay> personStays(@PathVariable long id){return service.personStays(id);}
 @GetMapping("/stays") List<Stay> stays(@RequestParam(required=false)String status,@RequestParam(required=false)Long buildingId,@RequestParam(required=false)String name){return service.stays(status,buildingId,name);}
 @GetMapping("/stays/{id}") Stay stay(@PathVariable long id){return service.stay(id);}
 @PostMapping("/stays/book") Stay book(@Valid @RequestBody BookCommand c,Principal p){return service.book(c,p.getName());}
 @PostMapping("/stays/{id}/check-in") Stay checkIn(@PathVariable long id,Principal p){return service.checkIn(id,p.getName());}
 @PostMapping("/stays/{id}/transfer") Stay transfer(@PathVariable long id,@Valid @RequestBody TransferCommand c,Principal p){return service.transfer(id,c,p.getName());}
 @PostMapping("/stays/{id}/extend") Stay extend(@PathVariable long id,@Valid @RequestBody ExtendCommand c,Principal p){return service.extend(id,c,p.getName());}
 @PostMapping("/stays/{id}/check-out") Stay checkout(@PathVariable long id,@Valid @RequestBody CheckoutCommand c,Principal p){return service.checkout(id,c,p.getName());}
 @PostMapping("/stays/{id}/cancel") Stay cancel(@PathVariable long id,Principal p){return service.cancel(id,p.getName());}
 @GetMapping("/stays/{id}/attachments") List<StayAttachment> attachments(@PathVariable long id){return extension.attachments(id);}
 @PostMapping(value="/stays/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) StayAttachment upload(@PathVariable long id,@RequestParam(defaultValue="OTHER")String type,@RequestPart MultipartFile file,Principal p){return extension.upload(id,type,file,p.getName());}
 @GetMapping("/attachments/{id}/download") ResponseEntity<Resource> download(@PathVariable long id){var d=extension.download(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.metadata().contentType())).contentLength(d.metadata().fileSize()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.metadata().originalName(),StandardCharsets.UTF_8).build().toString()).body(d.resource());}
 @DeleteMapping("/attachments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteAttachment(@PathVariable long id){extension.delete(id);}
 @GetMapping("/fees/rule") FeeRule feeRule(){return extension.feeRule();}
 @PutMapping("/fees/rule") FeeRule saveFeeRule(@Valid @RequestBody FeeRuleCommand c,Principal p){return extension.saveFeeRule(c,p.getName());}
 @GetMapping("/fees/bills") List<FeeBill> bills(@RequestParam String month){return extension.bills(month);}
 @PostMapping("/fees/generate") List<FeeBill> generateBills(@RequestParam String month,Principal p){return extension.generate(month,p.getName());}
 @PutMapping("/fees/bills/{id}") FeeBill updateBill(@PathVariable long id,@Valid @RequestBody FeeBillCommand c,Principal p){return extension.updateBill(id,c,p.getName());}
}
