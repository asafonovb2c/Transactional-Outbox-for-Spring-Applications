package com.asafonov.outbox.domain.event

/**
 * Status of the event in the outbox queue.
 * @property ENABLED Indicates that the event is active and will be processed.
 * @property DISABLED Indicates that the event is disabled after encountering an error.
*/
enum class OutboxEventStatus {
    ENABLED,
    DISABLED
}