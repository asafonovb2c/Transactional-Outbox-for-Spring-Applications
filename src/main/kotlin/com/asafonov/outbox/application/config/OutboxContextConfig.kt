package com.asafonov.outbox.application.config

import com.asafonov.outbox.application.OutboxEventHandleStrategy
import com.asafonov.outbox.application.OutboxEventProcessor
import com.asafonov.outbox.application.event.EventsProvider
import com.asafonov.outbox.application.event.OutboxSettingsManager
import com.asafonov.outbox.application.event.impl.DistributedEventsProvider
import com.asafonov.outbox.application.event.impl.OutboxEventFactory
import com.asafonov.outbox.application.event.impl.OutboxEventProcessorImpl
import com.asafonov.outbox.application.event.impl.OutboxEventStrategyCoordinator
import com.asafonov.outbox.application.event.impl.OutboxMetricsExportScheduler
import com.asafonov.outbox.application.event.impl.OutboxSettingsManagerImpl
import com.asafonov.outbox.application.event.impl.SingleInstanceEventsProvider
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.application.lock.impl.LocalOutboxKeyLocker
import com.asafonov.outbox.application.lock.impl.RedisOutboxKeyLocker
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.application.port.redis.RedisStashManager
import com.asafonov.outbox.domain.event.OutboxEventDto
import com.asafonov.outbox.out.repository.config.LockType
import com.asafonov.outbox.out.repository.event.OutboxEventDbAdapter
import io.micrometer.core.instrument.MeterRegistry
import mu.two.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.Environment
import org.springframework.data.redis.core.StringRedisTemplate


@ImportAutoConfiguration(value = [
        PrometheusMetricsExportAutoConfiguration::class,
        CompositeMeterRegistryAutoConfiguration::class,
        MetricsAutoConfiguration::class,
        MetricsEndpointAutoConfiguration::class
    ])
@Configuration
open class OutboxContextConfig {

    @Value("\${outbox.key.lockType:}")
    private val lockType: String? = null

    open val logger = KotlinLogging.logger {}

    @Bean
    open fun outboxKeyLocker(@Qualifier("outboxStringRedisTemplate") redisTemplate: StringRedisTemplate?):
            OutboxKeyLocker {
        logger.info("Outbox Lock type = $lockType")
        return when (lockType) {
            LockType.REDIS.name -> RedisOutboxKeyLocker(redisTemplate!!)
            LockType.LOCAL.name -> LocalOutboxKeyLocker()
            else -> throw IllegalStateException(
                "outbox.key.lock.type {$lockType} is not supported." +
                        " Supported types are LOCAL or REDIS "
            )
        }
    }

    @Bean
    open fun outboxEventFactory(): OutboxEventFactory {
        return OutboxEventFactory()
    }

    @Bean
    open fun outboxEventDbPort(@Qualifier("outboxSettingsManager") outboxEventSettings: OutboxSettingsManager,
                               @Qualifier("outboxEventFactory") outboxEventFactory: OutboxEventFactory):
            OutboxEventDbPort {
        return OutboxEventDbAdapter(outboxEventFactory, outboxEventSettings)
    }

    @Bean
    open fun outboxEventProvider(@Qualifier("outboxEventDbPort") outboxEventDbPort: OutboxEventDbPort,
                                 @Qualifier("outboxRedisStashManager") redisStash: RedisStashManager?):
            EventsProvider {
        return when (lockType) {
            LockType.REDIS.name -> DistributedEventsProvider(outboxEventDbPort, redisStash!!)
            LockType.LOCAL.name -> SingleInstanceEventsProvider(outboxEventDbPort)
            else -> throw IllegalStateException(
                "outbox.key.lock.type {$lockType} is not supported." +
                        " Supported types are LOCAL or REDIS "
            )
        }
    }

    @Bean
    open fun outboxSettingsManager(context: GenericApplicationContext,
                                   environment: Environment): OutboxSettingsManager {
        return OutboxSettingsManagerImpl(context, environment)
    }

    @Bean
    open fun outboxEventStrategyCoordinator(@Qualifier("outboxKeyLocker") locker: OutboxKeyLocker,
                                            metricRegistry: MeterRegistry): OutboxEventStrategyCoordinator {
        return OutboxEventStrategyCoordinator(locker, metricRegistry)
    }

    @Bean
    open fun outboxEventProcessor(@Qualifier("outboxEventDbPort") outboxEventDbPort: OutboxEventDbPort,
                                  @Qualifier("outboxEventStrategyCoordinator")
                                  coordinator: OutboxEventStrategyCoordinator,
                                  @Qualifier("outboxSettingsManager") outboxSettingsManager: OutboxSettingsManager,
                                  @Qualifier("outboxKeyLocker") locker: OutboxKeyLocker,
                                  @Qualifier("outboxEventProvider") eventsProvider: EventsProvider,
                                  @Qualifier("outboxRedisStashManager") redisStash: RedisStashManager?):
            OutboxEventProcessor {
        return OutboxEventProcessorImpl(
            outboxEventDbPort, coordinator, outboxSettingsManager, locker,
            eventsProvider, redisStash
        )
    }

    @Bean
    open fun outboxMetricsExportScheduler(@Qualifier("outboxEventDbPort") outboxEventDbPort: OutboxEventDbPort,
                                          @Qualifier("outboxKeyLocker") locker: OutboxKeyLocker,
                                          metricRegistry: MeterRegistry,
                                          handleStrategy: List<OutboxEventHandleStrategy<out OutboxEventDto>>):
            OutboxMetricsExportScheduler {
        return OutboxMetricsExportScheduler(outboxEventDbPort, metricRegistry, locker, handleStrategy)
    }
}