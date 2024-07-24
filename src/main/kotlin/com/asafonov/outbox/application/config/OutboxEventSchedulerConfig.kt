package com.asafonov.outbox.application.config

import com.asafonov.outbox.application.OutboxEventHandleStrategy
import com.asafonov.outbox.application.OutboxEventProcessor
import com.asafonov.outbox.application.event.OutboxSettingsManager
import com.asafonov.outbox.domain.event.OutboxEventDto
import mu.two.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar


@Configuration
@EnableScheduling
open class OutboxEventSchedulerConfig(
        open val handleStrategy: Collection<OutboxEventHandleStrategy<out OutboxEventDto>>,
        open val outboxSettingsManager: OutboxSettingsManager,
        open val outboxEventProcessorImpl: OutboxEventProcessor
) : SchedulingConfigurer, DisposableBean {

    open val logger = KotlinLogging.logger {}
    private var isDestroyed = false

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        handleStrategy.forEach { strategy ->
            if (isDestroyed) {
                return
            }

            taskRegistrar.addTriggerTask({ outboxEventProcessorImpl.process(strategy) },
                outboxSettingsManager.getTriggerOrAddIfAbsent(strategy.getEventType())
            )
        }
    }

    override fun destroy() {
        isDestroyed = true
        outboxSettingsManager.shutdownAllTaskExecutors()
    }

}