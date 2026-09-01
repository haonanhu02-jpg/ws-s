package com.wansheng.visitor.dormitory;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class DormitoryDatabaseMigration implements CommandLineRunner {
 private final DataSource dataSource;
 DormitoryDatabaseMigration(DataSource dataSource){this.dataSource=dataSource;}
 @Override public void run(String...args){Flyway.configure().dataSource(dataSource).schemas("dormitory_schema").defaultSchema("dormitory_schema").createSchemas(true).load().migrate();}
}
