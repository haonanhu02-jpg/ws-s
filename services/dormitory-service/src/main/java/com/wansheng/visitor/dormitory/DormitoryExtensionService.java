package com.wansheng.visitor.dormitory;

import static com.wansheng.visitor.dormitory.EmployeeDormitoryModels.*;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
class DormitoryExtensionService {
 private static final long MAX=10*1024*1024;private static final Set<String> TYPES=Set.of("APPLICATION/PDF","IMAGE/JPEG","IMAGE/PNG","APPLICATION/MSWORD","APPLICATION/VND.OPENXMLFORMATS-OFFICEDOCUMENT.WORDPROCESSINGML.DOCUMENT","APPLICATION/VND.MS-EXCEL","APPLICATION/VND.OPENXMLFORMATS-OFFICEDOCUMENT.SPREADSHEETML.SHEET");
 private final DormitoryExtensionRepository repo;private final EmployeeDormitoryService stays;private final Path root;
 DormitoryExtensionService(DormitoryExtensionRepository repo,EmployeeDormitoryService stays,@Value("${visitor.dormitory.attachment-path:./data/dormitory-attachments}")String path){this.repo=repo;this.stays=stays;this.root=Paths.get(path).toAbsolutePath().normalize();}
 List<StayAttachment> attachments(long stay){stays.stay(stay);return repo.attachments(stay);}
 StayAttachment upload(long stay,String type,MultipartFile file,String op){stays.stay(stay);if(file.isEmpty())throw bad("附件不能为空");if(file.getSize()>MAX)throw bad("附件不能超过10MB");String content=Optional.ofNullable(file.getContentType()).orElse("application/octet-stream").toUpperCase(Locale.ROOT);if(!TYPES.contains(content))throw bad("仅支持 PDF、图片、Word 和 Excel 文件");String original=Paths.get(Optional.ofNullable(file.getOriginalFilename()).orElse("attachment")).getFileName().toString();String stored=UUID.randomUUID()+extension(original);try{Files.createDirectories(root);Path target=root.resolve(stored).normalize();if(!target.startsWith(root))throw bad("非法文件名");file.transferTo(target);}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"附件保存失败");}long id=repo.addAttachment(stay,type==null||type.isBlank()?"OTHER":type.trim(),original,stored,content.toLowerCase(Locale.ROOT),file.getSize(),op);return repo.attachment(id).orElseThrow().metadata();}
 Download download(long id){var file=repo.attachment(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"附件不存在"));Path path=root.resolve(file.storedName()).normalize();if(!path.startsWith(root)||!Files.exists(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"附件文件不存在");return new Download(file.metadata(),new FileSystemResource(path));}
 @Transactional void delete(long id){var file=repo.attachment(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"附件不存在"));repo.deleteAttachment(id);try{Files.deleteIfExists(root.resolve(file.storedName()).normalize());}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"附件删除失败");}}
 @Transactional void deleteStay(long id,String op){Stay stay=stays.stay(id);if(stay.status()!=StayStatus.CANCELLED&&stay.status()!=StayStatus.CHECKED_OUT)throw conflict("只有已取消或已退宿记录可以删除");List<DormitoryExtensionRepository.AttachmentFile> files=repo.attachmentFiles(id);repo.deleteAttachments(id);stays.deleteTerminalStay(id,op);for(var file:files)try{Files.deleteIfExists(root.resolve(file.storedName()).normalize());}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"住宿记录已删除，但附件文件清理失败");}}
 FeeRule feeRule(){return repo.feeRule();}@Transactional FeeRule saveFeeRule(FeeRuleCommand c,String op){repo.saveFeeRule(c,op);return repo.feeRule();}
 List<FeeBill> bills(String month){validMonth(month);return repo.bills(month);}
 @Transactional List<FeeBill> generate(String month,String op){YearMonth current=validMonth(month),previous=current.minusMonths(1);FeeRule rule=repo.feeRule();if(!rule.enabled())throw conflict("费用结算规则尚未启用");for(var s:repo.feeSources(month,previous.toString())){BigDecimal water=usage(s.waterEnd(),s.previousWater()),electric=usage(s.electricEnd(),s.previousElectric());BigDecimal waterAmount=water.subtract(rule.freeWater()).max(BigDecimal.ZERO).multiply(rule.waterPrice()).setScale(2,RoundingMode.HALF_UP);BigDecimal electricAmount=electric.subtract(rule.freeElectric()).max(BigDecimal.ZERO).multiply(rule.electricPrice()).setScale(2,RoundingMode.HALF_UP);String occupants=String.join("、",repo.occupantNames(s.roomId(),current.atDay(1),current.plusMonths(1).atDay(1)));repo.saveBill(s,month,occupants,rule,water,electric,waterAmount,electricAmount,op);}return repo.bills(month);}
 @Transactional FeeBill updateBill(long id,FeeBillCommand c,String op){if(repo.updateBill(id,c,op)!=1)throw conflict("只有草稿账单可以调整或确认");return repo.bill(id).orElseThrow();}
 List<FeeSettlementEntry> settlements(String month,String person,Long buildingId){if(month!=null&&!month.isBlank())validMonth(month);return repo.settlementEntries(null,month,person,buildingId);}
 @Transactional FeeSettlementResult generateSettlements(String month,String op){
  YearMonth ym=validMonth(month);LocalDate start=ym.atDay(1),end=ym.plusMonths(1).atDay(1);String previous=ym.minusMonths(1).toString();
  Map<Long,DormitoryExtensionRepository.FeeSource> sources=new LinkedHashMap<>();for(var source:repo.feeSources(month,previous))sources.put(source.roomId(),source);
  List<DormitoryExtensionRepository.SettlementStay> staysInMonth=repo.settlementStays(start,end);long batchId=repo.createSettlementBatch(month,"GENERATED",null,"月底批量结算",op);
  for(var roomEntry:staysInMonth.stream().filter(s->sources.containsKey(s.roomId())).collect(java.util.stream.Collectors.groupingBy(DormitoryExtensionRepository.SettlementStay::roomId,LinkedHashMap::new,java.util.stream.Collectors.toList())).entrySet()){
   var source=sources.get(roomEntry.getKey());List<DormitoryExtensionRepository.SettlementStay> roomStays=roomEntry.getValue();Map<Long,Acc> acc=new LinkedHashMap<>();int occupiedRoomDays=0,maxOccupants=0;
   for(LocalDate day=start;day.isBefore(end);day=day.plusDays(1)){LocalDate currentDay=day;List<DormitoryExtensionRepository.SettlementStay> active=roomStays.stream().filter(s->active(s,currentDay)).toList();if(active.isEmpty())continue;occupiedRoomDays++;maxOccupants=Math.max(maxOccupants,active.size());BigDecimal weight=BigDecimal.ONE.divide(BigDecimal.valueOf(active.size()),10,RoundingMode.HALF_UP);for(var s:active)acc.computeIfAbsent(s.stayId(),k->new Acc(s)).add(weight);}
   if(occupiedRoomDays==0)continue;BigDecimal water=usage(source.waterEnd(),source.previousWater()),electric=usage(source.electricEnd(),source.previousElectric());int totalChargeableDays=0;
   for(var a:acc.values()){a.chargeable=chargeable(a.stay,month,end)?a.days:0;totalChargeableDays+=a.chargeable;}
   BigDecimal denominator=BigDecimal.valueOf(occupiedRoomDays);BigDecimal freeRoom=electric.min(new BigDecimal("5"));
   for(var a:acc.values()){
    BigDecimal waterShare=water.multiply(a.weight).divide(denominator,8,RoundingMode.HALF_UP);BigDecimal electricShare=electric.multiply(a.weight).divide(denominator,8,RoundingMode.HALF_UP);BigDecimal free=a.chargeable==0||totalChargeableDays==0?BigDecimal.ZERO:freeRoom.multiply(BigDecimal.valueOf(a.chargeable)).divide(BigDecimal.valueOf(totalChargeableDays),8,RoundingMode.HALF_UP);
    BigDecimal waterAmount=a.chargeable==0?BigDecimal.ZERO:waterShare.multiply(new BigDecimal("15")).setScale(2,RoundingMode.HALF_UP);BigDecimal electricAmount=a.chargeable==0?BigDecimal.ZERO:electricShare.subtract(free).max(BigDecimal.ZERO).multiply(new BigDecimal("0.7")).setScale(2,RoundingMode.HALF_UP);BigDecimal total=waterAmount.add(electricAmount);
    repo.addSettlementEntry(batchId,new DormitoryExtensionRepository.SettlementLine(a.stay.stayId(),a.stay.personId(),a.stay.roomId(),a.stay.buildingName(),a.stay.roomNo(),month,a.days,a.chargeable,maxOccupants,waterShare.setScale(4,RoundingMode.HALF_UP),electricShare.setScale(4,RoundingMode.HALF_UP),free.setScale(4,RoundingMode.HALF_UP),waterAmount,electricAmount,total),null);
   }
  }
  return result(batchId);
 }
 @Transactional FeeSettlementResult reverseSettlement(long batchId,FeeReversalCommand command,String op){FeeSettlementBatch source=repo.settlementBatch(batchId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"结算批次不存在"));if(!"GENERATED".equals(source.status()))throw conflict("只有正常结算批次可以冲正");if(repo.hasReversal(batchId))throw conflict("该结算批次已经冲正");long reversal=repo.createSettlementBatch(source.billingMonth(),"REVERSAL",batchId,command.reason(),op);for(var e:repo.settlementEntries(batchId,null,null,null)){repo.addSettlementEntry(reversal,new DormitoryExtensionRepository.SettlementLine(e.stayId(),e.personId(),e.roomId(),e.buildingName(),e.roomNo(),e.billingMonth(),-e.occupiedDays(),-e.chargeableDays(),e.occupantCount(),e.waterUsage().negate(),e.electricUsage().negate(),e.freeElectricUsage().negate(),e.waterAmount().negate(),e.electricAmount().negate(),e.totalAmount().negate()),e.id());}return result(reversal);}
 private FeeSettlementResult result(long batchId){return new FeeSettlementResult(repo.settlementBatch(batchId).orElseThrow(),repo.settlementEntries(batchId,null,null,null));}
 private boolean chargeable(DormitoryExtensionRepository.SettlementStay stay,String month,LocalDate monthEnd){if(repo.hasHistoricalPayment(stay.personId(),month))return true;LocalDate continuousEnd=stay.plannedMoveOut()==null||stay.plannedMoveOut().isAfter(monthEnd)?monthEnd:stay.plannedMoveOut();return ChronoUnit.DAYS.between(stay.plannedMoveIn(),continuousEnd)>=15;}
 private static boolean active(DormitoryExtensionRepository.SettlementStay stay,LocalDate day){return !day.isBefore(stay.plannedMoveIn())&&(stay.plannedMoveOut()==null||day.isBefore(stay.plannedMoveOut()));}
 private static BigDecimal usage(BigDecimal current,BigDecimal previous){if(current==null||previous==null)return BigDecimal.ZERO;return current.subtract(previous).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);}
 private static YearMonth validMonth(String month){try{return YearMonth.parse(month);}catch(Exception e){throw bad("月份格式应为 YYYY-MM");}}
 private static String extension(String name){int i=name.lastIndexOf('.');return i<0?"":name.substring(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]","");}
 private static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private static ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
 record Download(StayAttachment metadata,Resource resource){}
 private static final class Acc {final DormitoryExtensionRepository.SettlementStay stay;int days;int chargeable;BigDecimal weight=BigDecimal.ZERO;Acc(DormitoryExtensionRepository.SettlementStay stay){this.stay=stay;}void add(BigDecimal value){days++;weight=weight.add(value);}}
}
