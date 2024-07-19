package com.asafonov.outbox.application.port.out


import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventDto
import com.asafonov.outbox.domain.event.OutboxEventType
import com.asafonov.outbox.domain.metric.OutboxEventTypeMetricsDto
import java.time.Instant

/**
 * Port for handling queue events in the event system.
 */
interface OutboxEventDbPort {
    /**
     * Deletes events from the queue.
     *
     * @param events The list of events to be deleted from the queue.
     */
    fun delete(events: List<OutboxEvent>)

    /**
     * Saves new events to the queue.
     *
     * @param type The type of event.
     * @param runtime The time when the event should be processed.
     * @param events The set of events to be saved.
     */
    fun saveNewEvents(type: OutboxEventType, runtime: Instant, events: Set<OutboxEventDto>)

    /**
     * Updates existing events in the queue.
     *
     * @param events The list of events to be updated.
     */
    fun update(events: List<OutboxEvent>)

    /**
     * Retrieves events from the queue based on specified parameters.
     *
     * @param eventType The type of events to retrieve.
     * @param attemptsMax The maximum number of processing attempts allowed.
     * @param limit The maximum number of events to retrieve.
     * @return A collection of all found events that match the specified parameters.
     */
    fun selectEnabledEvents(eventType: OutboxEventType, attemptsMax: Long, limit: Int): Collection<OutboxEvent>

    /**
     * Retrieves events from the queue based on specified parameters,
     * excluding those that are already being processed.
     *
     * @param eventType The type of events to retrieve.
     * @param attemptsMax The maximum number of processing attempts allowed.
     * @param limit The maximum number of events to retrieve.
     * @param excludedUuids The UUIDs of events that are already in processing.
     * @return A collection of all found events that match the specified parameters.
     */
    fun selectEnabledEventsWithoutExcluded(
            eventType: OutboxEventType,
            attemptsMax: Long,
            limit: Int,
            excludedUuids: Collection<String>
    ): Collection<OutboxEvent>

    /**
     * Saves a new event to the queue.
     *
     * @param eventType The type of the event.
     * @param eventDto The event data transfer object to be saved.
     * @param runtime The time when the event should be processed.
     */
    fun saveNewEvent(eventType: OutboxEventType, eventDto: OutboxEventDto, runtime: Instant)

    /**
     * Exports metrics for queue events.
     *
     * @return A list of metrics for exporting to Grafana.
     */
    fun getOutboxEventMetrics(): List<OutboxEventTypeMetricsDto>
}