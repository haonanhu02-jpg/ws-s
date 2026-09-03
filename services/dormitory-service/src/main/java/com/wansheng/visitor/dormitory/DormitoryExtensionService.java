package com.wansheng.visitor.dormitory;

import static com.wansheng.visitor.dormitory.EmployeeDormitoryModels.*;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.YearMonth;
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
 FeeRule feeRule(){return repo.feeRule();}@Transactional FeeRule saveFeeRule(FeeRuleCommand c,String op){repo.saveFeeRule(c,op);return repo.feeRule();}
 List<FeeBill> bills(String month){validMonth(month);return repo.bills(month);}
 @Transactional List<FeeBill> generate(String month,String op){YearMonth current=validMonth(month),previous=current.minusMonths(1);FeeRule rule=repo.feeRule();if(!rule.enabled())throw conflict("费用结算规则尚未启用");for(var s:repo.feeSources(month,previous.toString())){BigDecimal water=usage(s.waterEnd(),s.previousWater()),electric=usage(s.electricEnd(),s.previousElectric());BigDecimal waterAmount=water.subtract(rule.freeWater()).max(BigDecimal.ZERO).multiply(rule.waterPrice()).setScale(2,RoundingMode.HALF_UP);BigDecimal electricAmount=electric.subtract(rule.freeElectric()).max(BigDecimal.ZERO).multiply(rule.electricPrice()).setScale(2,RoundingMode.HALF_UP);String occupants=String.join("、",repo.occupantNames(s.roomId(),current.atDay(1),current.plusMonths(1).atDay(1)));repo.saveBill(s,month,occupants,rule,water,electric,waterAmount,electricAmount,op);}return repo.bills(month);}
 @Transactional FeeBill updateBill(long id,FeeBillCommand c,String op){if(repo.updateBill(id,c,op)!=1)throw conflict("只有草稿账单可以调整或确认");return repo.bill(id).orElseThrow();}
 private static BigDecimal usage(BigDecimal current,BigDecimal previous){if(current==null||previous==null)return BigDecimal.ZERO;return current.subtract(previous).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);}
 private static YearMonth validMonth(String month){try{return YearMonth.parse(month);}catch(Exception e){throw bad("月份格式应为 YYYY-MM");}}
 private static String extension(String name){int i=name.lastIndexOf('.');return i<0?"":name.substring(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]","");}
 private static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private static ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
 record Download(StayAttachment metadata,Resource resource){}
}
