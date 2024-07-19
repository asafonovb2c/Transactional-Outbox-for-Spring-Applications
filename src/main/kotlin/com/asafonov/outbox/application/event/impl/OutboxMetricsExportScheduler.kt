package com.asafonov.outbox.application.event.impl

import com.asafonov.outbox.application.OutboxEventHandleStrategy
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.domain.event.OutboxEventDto
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.atomic.AtomicLong



open class OutboxMetricsExportScheduler (
        open val outboxEventDbPort: OutboxEventDbPort,
        open val metricRegistry: MeterRegistry,
        open val outboxKeyLocker: OutboxKeyLocker,
        open val handleStrategy: List<OutboxEventHandleStrategy<out OutboxEventDto>>)
{

    open val logger = KotlinLogging.logger {}

    /**
     * Stores the latest queue size data for reporting to Prometheus.
     * This is done to prevent NaN values from being returned.
     * See the documentation for micrometer gauges.
     * This is a workaround for connecting to Java.
     */
    companion object {
        val eventSizeCache: MutableMap<String, AtomicLong> = HashMap()
        const val LOCK_KEY: String = "exportOutboxMetrics"
        const val LOCK_TIMEOUT: Long = 55000
    }

    @Scheduled(cron = "\${export.outbox.event.state.metrics:0 0/1 * * * *}")
    fun exportMetrics() {
        val locked = outboxKeyLocker.tryLockWithTimeOut(LOCK_KEY, LOCK_TIMEOUT)

        if (!locked) {
            return
        }

        try {
            val currentSizeMetrics: Map<String, Long>  = outboxEventDbPort.getOutboxEventMetrics().associate { it.type to it.size }

            handleStrategy.forEach { strategy ->
                val name = strategy.getEventType().getName()
                val newValue: Long? = currentSizeMetrics[name]
                eventSizeCache[name] = AtomicLong(newValue ?: 0)
                metricRegistry.gauge("outbox.event.size",
                    listOf(Tag.of("type", name.lowercase())), eventSizeCache[name]!!)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to export outbox metrics"}
            throw e
        } finally {
            outboxKeyLocker.unlock(LOCK_KEY, locked)
        }
    }
}