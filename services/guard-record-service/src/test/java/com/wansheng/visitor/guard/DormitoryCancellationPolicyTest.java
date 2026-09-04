package com.wansheng.visitor.guard;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class DormitoryCancellationPolicyTest {
 @Test void confirmedRequestsAreBlocked(){check("{\"allowed\":false}",409);}
 @Test void pendingRequestsAreAllowed(){check("{\"allowed\":true}",0);}
 private void check(String body,int status){var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();server.expect(requestTo("http://dorm/internal/dormitory/records/V1/cancellation-status")).andExpect(header("X-Internal-Token","test-token")).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));var policy=new DormitoryCancellationPolicy(builder,"http://dorm","test-token");if(status==0)policy.check("V1");else assertThatThrownBy(()->policy.check("V1")).isInstanceOfSatisfying(ResponseStatusException.class,e->org.assertj.core.api.Assertions.assertThat(e.getStatusCode().value()).isEqualTo(status));server.verify();}
 @Test void outageDoesNotPermitCancellation(){var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();server.expect(requestTo("http://dorm/internal/dormitory/records/V1/cancellation-status")).andRespond(withServerError());var policy=new DormitoryCancellationPolicy(builder,"http://dorm","test-token");assertThatThrownBy(()->policy.check("V1")).isInstanceOfSatisfying(ResponseStatusException.class,e->org.assertj.core.api.Assertions.assertThat(e.getStatusCode().value()).isEqualTo(503));server.verify();}
}
