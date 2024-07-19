package com.asafonov.outbox.out.repository.config

import org.flywaydb.core.Flyway
import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource


@Configuration
@MapperScan(basePackages = ["com.asafonov.outbox.out.repository"])
open class FlywayOutboxConfiguration{

    private val migrationLocation: String = "classpath:outbox/migrations/"

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean(name = ["outboxFlyway"])
    open fun outboxFlyway(dataSource: DataSource): Flyway {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations(migrationLocation)
            .schemas("outbox")
            .load()
    }
}