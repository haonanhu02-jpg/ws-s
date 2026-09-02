package com.wansheng.visitor.registration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class RegistrationDatabaseMigration implements CommandLineRunner {
    private final DataSource dataSource;

    RegistrationDatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("registration_schema")
                .defaultSchema("registration_schema")
                .createSchemas(true)
                .load()
                .migrate();
    }
}
