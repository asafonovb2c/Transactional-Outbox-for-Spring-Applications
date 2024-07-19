package com.asafonov.outbox.domain.event

/**
 * Interface and contract of queue events.
 */
interface OutboxEventType {
    /**
     * Returns the name of the event.
     */
    fun getName(): String

}