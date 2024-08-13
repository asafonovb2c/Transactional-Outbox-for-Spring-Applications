package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.application.event.impl.OutboxMetricsExportScheduler
import com.asafonov.outbox.application.event.impl.TestOutboxEventHandleStrategy
import com.asafonov.outbox.application.event.impl.TestOutboxEventHandleStrategySecond
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.domain.metric.OutboxEventTypeMetricsDto
import com.asafonov.outbox.domain.TestOutboxEventType
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

class OutboxMetricsExportSchedulerTest {

    private val outboxKeyLocker: OutboxKeyLocker = spyk<OutboxKeyLocker>()
    private val metricRegistry: MeterRegistry = spyk<CompositeMeterRegistry>()
    private val outboxEventDbPort: OutboxEventDbPort = spyk<OutboxEventDbPort>()
    private val strategies = listOf(TestOutboxEventHandleStrategy(), TestOutboxEventHandleStrategySecond())

    @InjectMockKs
    private val outboxMetricsExportScheduler = OutboxMetricsExportScheduler(outboxEventDbPort, metricRegistry,
        outboxKeyLocker, strategies)

    private val metrics = listOf(OutboxEventTypeMetricsDto(TestOutboxEventType.TEST_EVENT.getName(), 5))

    @Test
    @DisplayName("Checking that metrics are exported correctly")
    fun exportMetric_getMetrics_verifyCount() {
        every { outboxEventDbPort.getOutboxEventMetrics() } returns metrics
        every { outboxKeyLocker.tryLockWithTimeOut(any(), any()) }  returns true

        outboxMetricsExportScheduler.initMetrics()

        verify { metricRegistry.gauge<AtomicLong>("outbox.event.size",
            listOf(Tag.of("type", TestOutboxEventType.TEST_EVENT.getName().lowercase())), match { it.get() == 5L }) }
    }

}