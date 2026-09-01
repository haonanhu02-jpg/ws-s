package com.wansheng.visitor.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class AccountRepositoryTest {
    @Test
    void managesPersistentAccounts() throws Exception {
        var h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:auth;MODE=PostgreSQL");
        try (var dataSource = new SingleConnectionDataSource(h2.getConnection(), true)) {
            Flyway.configure().dataSource(dataSource).load().migrate();
            var repo = new AccountRepository(new JdbcTemplate(dataSource));

            repo.create("guard-02", "二号门卫", "{noop}password", "GUARD");
            assertThat(repo.loadUser("guard-02").isEnabled()).isTrue();
            assertThat(repo.update("guard-02", "夜班门卫", "GUARD", false)).isOne();
            assertThat(repo.find("guard-02").orElseThrow().enabled()).isFalse();
            assertThat(repo.delete("guard-02")).isOne();
        }
    }
}
