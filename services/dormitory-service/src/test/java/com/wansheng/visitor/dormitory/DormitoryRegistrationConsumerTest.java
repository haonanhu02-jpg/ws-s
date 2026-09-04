package com.wansheng.visitor.dormitory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DormitoryRegistrationConsumerTest {
 @Test void qrEditPreservesRegistrationMetadata()throws Exception{
  var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();var repo=mock(DormitoryRepository.class);
  server.expect(requestTo("http://guard/internal/guard/records/V1/dormitory-view")).andRespond(withSuccess("{\"visitId\":\"V1\",\"visitorName\":\"新姓名\",\"mobile\":\"13800138000\",\"hostName\":\"李经理\",\"accommodationRequired\":true,\"vehicleEnteringFactory\":false,\"detailsVersion\":3}",MediaType.APPLICATION_JSON));
  server.expect(requestTo("http://registration/internal/registrations/V1/dormitory-view")).andRespond(withSuccess("{\"visitId\":\"V1\",\"hostDepartment\":\"生产部\",\"visitReason\":\"设备维护\",\"hasVehicle\":true,\"accommodationRequired\":true,\"vehicleEnteringFactory\":false}",MediaType.APPLICATION_JSON));
  new DormitoryRegistrationConsumer(new ObjectMapper(),b,repo,"http://registration","http://guard","token",DormitoryScopeMode.ACCOMMODATION_ONLY).receiveGuardManual("{\"eventId\":\"E1\",\"visitId\":\"V1\"}");
  var record=ArgumentCaptor.forClass(DormitoryRecord.class);verify(repo).syncGuard(eq("E1"),record.capture(),eq(3L),any(Instant.class));
  assertThat(record.getValue().visitorName()).isEqualTo("新姓名");assertThat(record.getValue().hostDepartment()).isEqualTo("生产部");assertThat(record.getValue().visitReason()).isEqualTo("设备维护");assertThat(record.getValue().hasVehicle()).isTrue();server.verify();
 }
 @Test void cancellationIsDeliveredAndConflictIsCompensated()throws Exception{
  var b=RestClient.builder();var server=MockRestServiceServer.bindTo(b).build();var repo=mock(DormitoryRepository.class);
  when(repo.syncGuard(anyString(),any(),anyLong(),any())).thenReturn(true);
  server.expect(requestTo("http://guard/internal/guard/records/GUARD-1/dormitory-view")).andRespond(withSuccess("{\"visitId\":\"GUARD-1\",\"visitorName\":\"张三\",\"mobile\":\"13800138000\",\"hostName\":\"李经理\",\"accommodationRequired\":false,\"vehicleEnteringFactory\":false,\"detailsVersion\":4}",MediaType.APPLICATION_JSON));
  server.expect(requestTo("http://guard/internal/guard/records/GUARD-1/reject-accommodation-cancellation")).andExpect(method(HttpMethod.POST)).andExpect(content().json("{\"detailsVersion\":4}")).andRespond(withSuccess());
  new DormitoryRegistrationConsumer(new ObjectMapper(),b,repo,"http://registration","http://guard","token",DormitoryScopeMode.ACCOMMODATION_ONLY).receiveGuardManual("{\"eventId\":\"E1\",\"visitId\":\"GUARD-1\"}");
  verify(repo).syncGuard(eq("E1"),argThat(r->!r.accommodationRequired()),eq(4L),any());server.verify();
 }
}
