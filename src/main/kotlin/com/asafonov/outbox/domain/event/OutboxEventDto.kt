package com.asafonov.outbox.domain.event

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Interface and the contract of an event in the event queue.
 */
interface OutboxEventDto {
    /**
     * Each event must have a lock key.
     *
     * @return The lock key associated with the event.
     */
    @JsonIgnore
    fun provideLockKey(): String
}