package com.asafonov.outbox.application.event

import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventType

/**
 * Interface for handling the reception and processing of events.
 */
interface EventsProvider {
    /**
     * Retrieves events for processing.
     *
     * @param eventType The type of queue from which to retrieve events.
     * @param settings The configuration settings for the queue.
     * @param sessionUuid The UUID associated with the processing session.
     */
    fun getEvents(eventType: OutboxEventType, settings: OutboxEventSettings, sessionUuid: String): Collection<OutboxEvent>
}