package com.wansheng.visitor.guard;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class GuardDatabaseMigration implements CommandLineRunner {
    private final DataSource dataSource;

    GuardDatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("guard_schema")
                .defaultSchema("guard_schema")
                .createSchemas(true)
                .load()
                .migrate();
    }
}
