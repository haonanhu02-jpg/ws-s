package com.wansheng.visitor.guard;
import java.time.Clock; import java.util.List;
import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException;
@Service
class GuardService {
  private final GuardRepository repo; private final Clock clock=Clock.systemUTC();
  GuardService(GuardRepository repo){this.repo=repo;}
  List<GuardRecord> list(GuardStatus s){return repo.list(s);} GuardRecord find(String id){return repo.find(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"门卫记录不存在"));}
  @Transactional GuardRecord entry(String id,String op){transition(id,GuardStatus.WAITING_ENTRY,GuardStatus.IN_FACTORY,op);return find(id);}
  @Transactional GuardRecord exit(String id,String op){transition(id,GuardStatus.IN_FACTORY,GuardStatus.EXITED,op);return find(id);}
  private void transition(String id,GuardStatus f,GuardStatus t,String op){if(repo.transition(id,f,t,op,clock.instant())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"当前状态不允许该操作");}
}

