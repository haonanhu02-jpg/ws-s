package com.wansheng.visitor.guard;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/visitor/guard/records")
class GuardController {
 private final GuardService service; private final GuardVisibilityMode visibility;
 GuardController(GuardService s,@Value("${visitor.guard.visibility-mode:IMMEDIATE}") GuardVisibilityMode visibility){service=s;this.visibility=visibility;}
 @GetMapping List<GuardRecord> list(@RequestParam GuardStatus status){if(visibility==GuardVisibilityMode.UNDECIDED)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Q-008 尚未确认");return service.list(status);}
 @GetMapping("/{visitId}") GuardRecord find(@PathVariable String visitId){return service.find(visitId);}
 @PostMapping("/{visitId}/entry") GuardRecord entry(@PathVariable String visitId,@RequestHeader("X-Operator") String op){return service.entry(visitId,op);}
 @PostMapping("/{visitId}/exit") GuardRecord exit(@PathVariable String visitId,@RequestHeader("X-Operator") String op){return service.exit(visitId,op);}
}
