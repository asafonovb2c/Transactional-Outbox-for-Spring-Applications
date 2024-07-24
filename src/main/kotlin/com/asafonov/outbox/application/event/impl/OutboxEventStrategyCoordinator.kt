package com.asafonov.outbox.application.event.impl

import com.asafonov.outbox.application.OutboxEventHandleStrategy
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.domain.metric.EventProcessingMetrics
import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventDto
import com.asafonov.outbox.domain.event.OutboxEventResult
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.createEventLockBusyResult
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import mu.two.KotlinLogging

open class OutboxEventStrategyCoordinator(open val outboxKeyLocker: OutboxKeyLocker,
                                          open val metricRegistry: MeterRegistry) {

    open  val logger = KotlinLogging.logger {}

    /**
     * Applies the selected processing strategy to an event from the queue.
     * @param strategy The event handler that defines the processing strategy.
     * @param event The event retrieved from the queue.
     * @return The result of processing the event.
     */
    suspend fun <T: OutboxEventDto> applyEventStrategy(strategy: OutboxEventHandleStrategy<T>, event: OutboxEvent,
                                                       settings: OutboxEventSettings): OutboxEventResult {
        val timerSample = Timer.start(metricRegistry)
        var locked = false

        try {
            val eventDto: T = strategy.convertToEventDto(event)
            locked = outboxKeyLocker.tryLockWithTimeOutAsync(event.lockKey, settings.timeout)

            if (!locked) {
                return createEventLockBusyResult(event.lockKey)
            }

            return strategy.handleEvent(eventDto)
        } catch (exception: Exception) {
            metricRegistry.counter(EventProcessingMetrics.METRIC_ERROR_NAME, strategy.getMetricTags()).increment()
            logger.error(("Failed to apply ${strategy.getEventType().getName()} for $event"))
            throw exception
        } finally {
            timerSample.stop(metricRegistry.timer(EventProcessingMetrics.METRIC_BASE_NAME, strategy.getMetricTags()))
            outboxKeyLocker.unlockAsync(event.lockKey, locked)
        }
    }
}