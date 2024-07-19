package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.application.event.impl.SingleInstanceEventsProvider
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.TestOutboxEventType
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import java.util.UUID

open class SingleInstanceEventsProviderTest {

    private val outboxEventDbPort: OutboxEventDbPort = spyk<OutboxEventDbPort>()
    private val singleInstanceEventsProvider: SingleInstanceEventsProvider = SingleInstanceEventsProvider(outboxEventDbPort)

    private val eventType = TestOutboxEventType.TEST_EVENT
    private val mockEnvironment = MockEnvironment()
    private val settings: OutboxEventSettings = OutboxEventSettings(eventType.getName(), mockEnvironment)

    @Test
    @DisplayName("Test Single instance events provider test")
    fun getEvents_simpleCallVerify_allOk() {
       singleInstanceEventsProvider.getEvents(eventType, settings, UUID.randomUUID().toString())
       verify {  outboxEventDbPort.selectEnabledEvents(eventType, settings.attemptsMax, settings.loadEventsBatch) }
    }

}