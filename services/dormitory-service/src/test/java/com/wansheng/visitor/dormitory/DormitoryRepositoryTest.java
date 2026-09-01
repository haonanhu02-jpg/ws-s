package com.wansheng.visitor.dormitory;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;import org.flywaydb.core.Flyway;import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.*;import org.springframework.jdbc.core.JdbcTemplate;
class DormitoryRepositoryTest{
 private JdbcTemplate jdbc;private DormitoryRepository repo;
 @BeforeEach void setup(){JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:dorm;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");Flyway.configure().dataSource(ds).cleanDisabled(false).load().clean();Flyway.configure().dataSource(ds).load().migrate();jdbc=new JdbcTemplate(ds);repo=new DormitoryRepository(jdbc);jdbc.update("INSERT INTO dormitory_record(visit_id,visitor_name,mobile,host_name,host_department,visit_reason,accommodation_required,has_vehicle,vehicle_entering_factory,accommodation_confirmed,created_at) VALUES('V1','张三','13800138000','李经理','生产部','交流',TRUE,FALSE,FALSE,TRUE,CURRENT_TIMESTAMP)");repo.addBed("1号楼","101","1-101-A");repo.addBed("1号楼","101","1-101-B");}
 @Test void assignsAndChangesBedWithAudit(){Instant t=Instant.parse("2026-08-24T01:00:00Z");assertThat(repo.assign("V1","1-101-A","dorm-a",t)).isOne();assertThat(repo.assign("V1","1-101-B","dorm-a",t.plusSeconds(60))).isOne();assertThat(repo.find("V1").orElseThrow().bedCode()).isEqualTo("1-101-B");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_change_audit",Integer.class)).isEqualTo(2);}
}

