package com.wansheng.visitor.guard;

import java.time.Instant;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** One-time, restartable repair of pre-versioned projections; never overwrites guard edits. */
@Component
class GuardAccommodationReconciler {
    private final GuardRepository repo;
    private final RestClient client;
    private final String url,token;
    GuardAccommodationReconciler(GuardRepository repo,RestClient.Builder builder,
            @Value("${visitor.guard.registration-service-url}")String url,
            @Value("${visitor.guard.internal-token}")String token){this.repo=repo;client=builder.build();this.url=url;this.token=token;}
    @Scheduled(initialDelay=30000,fixedDelay=30000)
    void reconcile(){
        for(String id:repo.pendingAccommodationSync()){
            try {
                Boolean required=null;
                var current=repo.dormitoryView(id).orElseThrow();
                if(!id.startsWith("GUARD-")&&current.detailsVersion()==0){
                    var source=client.get().uri(url+"/internal/registrations/{id}/guard-view",id).header("X-Internal-Token",token).retrieve().body(GuardRegistrationConsumer.GuardView.class);
                    if(source==null)throw new IllegalStateException("missing registration");
                    required=source.accommodationRequired();
                }
                repo.reconcileAccommodation(id,required,Instant.now());
            }catch(Exception ex){LoggerFactory.getLogger(getClass()).warn("Accommodation projection repair will retry visit {} ({})",id,ex.getClass().getSimpleName());}
        }
    }
}
