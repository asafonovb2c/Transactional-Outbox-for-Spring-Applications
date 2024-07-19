package com.asafonov.outbox.out.repository.event

import com.asafonov.outbox.application.event.impl.JacksonSerializer
import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.domain.TestEventDto
import com.asafonov.outbox.domain.TestOutboxEventType
import java.time.Instant
import java.util.UUID

fun createNewEvent() : OutboxEvent {
    return OutboxEvent(UUID.randomUUID().toString(), TestOutboxEventType.TEST_EVENT.name, Instant.now(), Instant.now(),
        JacksonSerializer.serialize(TestEventDto(UUID.randomUUID().toString())),
            0, null, "lockKey", OutboxEventStatus.ENABLED)
}