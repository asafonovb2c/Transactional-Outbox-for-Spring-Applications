package com.asafonov.outbox.domain.event

/**
 * Interface and contract of queue events.
 */
interface OutboxEventType {
    /**
     * Отдать имя события
     */
    fun getName(): String

}