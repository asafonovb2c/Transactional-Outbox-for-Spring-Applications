package com.asafonov.outbox.application.event.impl

import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventDto
import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.domain.event.OutboxEventType
import mu.two.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
open class OutboxEventFactory {

    open val logger = KotlinLogging.logger {}

    fun createNewEvent(type: OutboxEventType, event: OutboxEventDto, runTime: Instant): OutboxEvent {
        try {
            val jsonizedEvent: String = JacksonSerializer.serialize(event)

            return OutboxEvent(UUID.randomUUID().toString(), type.getName(), Instant.now(), runTime, jsonizedEvent,
                0,null, event.provideLockKey(), OutboxEventStatus.ENABLED)
        } catch (e: Exception) {
            logger.error("Failed to convert object $event  to outboxEvent", e)
            throw e
        }
    }
}