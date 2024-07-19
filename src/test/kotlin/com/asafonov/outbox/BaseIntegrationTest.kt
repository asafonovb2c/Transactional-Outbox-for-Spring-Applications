package com.asafonov.outbox

import com.asafonov.outbox.application.config.OutboxContextConfig
import com.asafonov.outbox.application.config.OutboxEventSchedulerConfig
import com.asafonov.outbox.application.config.OutboxRedisConfig
import com.asafonov.outbox.out.repository.AdditionalDaoConfig
import com.asafonov.outbox.out.repository.config.FlywayOutboxConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@TestPropertySource(locations = ["/application.properties"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [
        OutboxContextConfig::class,
        FlywayOutboxConfiguration::class,
        AdditionalDaoConfig::class,
        OutboxRedisConfig::class,
        OutboxEventSchedulerConfig::class
    ])
open class BaseIntegrationTest