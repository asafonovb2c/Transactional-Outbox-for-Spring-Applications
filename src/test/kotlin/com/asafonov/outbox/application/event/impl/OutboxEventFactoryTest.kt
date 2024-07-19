package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.application.event.impl.JacksonSerializer
import com.asafonov.outbox.application.event.impl.OutboxEventFactory
import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.domain.TestEventDto
import com.asafonov.outbox.domain.TestOutboxEventType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class OutboxEventFactoryTest {

    val outboxEventFactory = OutboxEventFactory()

    @Test
    @DisplayName("Run All Operations for a Single Queue Type")
    fun createNewEvent() {

        val event = TestEventDto("key")
        val lockKey = event.provideLockKey()
        val now = Instant.now()
        val type = TestOutboxEventType.TEST_EVENT
        val jsonizedEvent: String = JacksonSerializer.serialize(event)

        val outboxEvent = outboxEventFactory.createNewEvent(type, event, now)

        Assertions.assertEquals(outboxEvent.event, jsonizedEvent)
        Assertions.assertEquals(outboxEvent.attempts, 0)
        Assertions.assertEquals(outboxEvent.lockKey, lockKey)
        Assertions.assertEquals(outboxEvent.status, OutboxEventStatus.ENABLED)
    }
}