package com.wansheng.visitor.guard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
class DormitoryCancellationPolicy {
    private final RestClient client;
    private final String url,token;
    DormitoryCancellationPolicy(RestClient.Builder builder,
            @Value("${visitor.guard.dormitory-service-url:http://localhost:8084}") String url,
            @Value("${visitor.guard.internal-token}") String token){this.client=builder.build();this.url=url;this.token=token;}
    void check(String id){
        CancellationStatus result;
        try {
            result=client.get().uri(url+"/internal/dormitory/records/{id}/cancellation-status",id)
                    .header("X-Internal-Token",token).retrieve().body(CancellationStatus.class);
        } catch(RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"暂时无法核验宿舍状态，请稍后再取消住宿");}
        if(result==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"宿舍状态未返回，请稍后重试");
        if(!result.allowed())throw new ResponseStatusException(HttpStatus.CONFLICT,"住宿已确认或已分配床位，请交宿舍管理员处理");
    }
    record CancellationStatus(boolean allowed) {}
}
