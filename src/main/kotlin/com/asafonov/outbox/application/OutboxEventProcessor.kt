package com.asafonov.outbox.application

import com.asafonov.outbox.domain.event.OutboxEventDto

/**
 * Processor for handling events from the event queue.
 */
interface OutboxEventProcessor {
    /**
     * Processes events from the queue using the selected strategy.
     *
     * @param strategy The chosen strategy for handling events.
     * @param <T> The type of event associated with the chosen strategy.
     */
    fun <T : OutboxEventDto> process(strategy: OutboxEventHandleStrategy<T>)
}