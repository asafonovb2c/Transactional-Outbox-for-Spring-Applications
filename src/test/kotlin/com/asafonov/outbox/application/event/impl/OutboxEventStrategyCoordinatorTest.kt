package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.application.event.impl.OutboxEventStrategyCoordinator
import com.asafonov.outbox.application.event.impl.TestOutboxEventHandleStrategy
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.TestOutboxEventType
import com.asafonov.outbox.out.repository.event.createNewEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class OutboxEventStrategyCoordinatorTest {

    private val outboxKeyLocker: OutboxKeyLocker = spyk<OutboxKeyLocker>()
    private val metricRegistry: MeterRegistry = CompositeMeterRegistry()

    private val outboxEventStrategyCoordinator = OutboxEventStrategyCoordinator(outboxKeyLocker, metricRegistry)
    private val strategy = TestOutboxEventHandleStrategy()
    private val eventType = TestOutboxEventType.TEST_EVENT
    private val event = createNewEvent()
    private val mockEnvironment = MockEnvironment()
    private val settings: OutboxEventSettings = OutboxEventSettings(eventType.getName(), mockEnvironment)

    @Test
    @DisplayName("Run All Operations for a Single Queue Type")
    fun applyEventStrategy_getLock_allOk() {
        runBlocking {
            launch(Dispatchers.Default) {
                coEvery { outboxKeyLocker.tryLockWithTimeOutAsync(event.lockKey, settings.timeout) } returns true

                outboxEventStrategyCoordinator.applyEventStrategy(strategy, event, settings)

                coVerify { outboxKeyLocker.unlockAsync(event.lockKey, any()) }
            }
        }
    }
}