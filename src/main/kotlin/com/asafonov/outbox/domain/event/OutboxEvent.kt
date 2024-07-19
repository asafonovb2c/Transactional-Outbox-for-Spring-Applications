package com.asafonov.outbox.domain.event


import java.time.Instant

/**
 * Represents an event from the queue.
 *
 * @property uuid The ID of the event.
 * @property eventType The type of the queue.
 * @property createTimestamp The time the event was created.
 * @property runTime The time when the event will be processed.
 * @property event The body of the event in JSON format.
 * @property attempts The number of retry attempts for processing.
 * @property failReason The reason for the last retry failure.
 * @property lockKey The lock key associated with the entity.
 * @property status The status of the event.
 */
data class OutboxEvent(
        var uuid: String,
        var eventType: String,
        var createTimestamp: Instant,
        var runTime: Instant,
        var event: String?,
        var attempts: Long,
        var failReason: String?,
        var lockKey: String,
        var status: OutboxEventStatus
)
