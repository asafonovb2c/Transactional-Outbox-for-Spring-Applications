package com.asafonov.outbox.application

import com.asafonov.outbox.application.event.impl.JacksonSerializer
import com.asafonov.outbox.domain.metric.EventProcessingMetrics
import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventDto
import com.asafonov.outbox.domain.event.OutboxEventResult
import com.asafonov.outbox.domain.event.OutboxEventType
import io.micrometer.core.instrument.Tag

/**
 * Interface contract of event queue handlers.
 * @param <T> The type of the event DTO.
 */
interface OutboxEventHandleStrategy<T : OutboxEventDto> {

    /**
     * Converts the event to a DTO.
     * @param event The event object from the queue.
     * @return The text from `OutboxEvent.getEvent()` converted to the specific class T.
     * @throws IllegalArgumentException If the event is null or empty.
     */
    fun convertToEventDto(event: OutboxEvent): T {
        if (event.event.isNullOrEmpty()) {
            throw IllegalArgumentException("Event in OutboxEvent must not be empty for ${this.getEventClass()}")
        }
        return JacksonSerializer.deserialize(event.event!!, this.getEventClass())
    }

    /**
     * Handles a specific event.
     * @param eventDto The event of type T.
     * @return The result of processing the event.
     */
    fun handleEvent(eventDto: T): OutboxEventResult

    /**
     * Returns a set of metric tags for monitoring event processing.
     * @return A list of tags for metrics.
     */
    fun getMetricTags(): Collection<Tag> {
        return listOf(
            Tag.of(EventProcessingMetrics.TAG_EVENT_TYPE_NAME, this.getEventType().getName().lowercase()),
            Tag.of(EventProcessingMetrics.TAG_EVENT_GROUP_NAME, "outbox")
        )
    }

    /**
     * Returns the type of event that the specific processor handles.
     * @return The type of event.
     */
    fun getEventType(): OutboxEventType

    /**
     * Returns the class of the event that the specific processor handles.
     * @return The class of the event.
     */
    fun getEventClass(): Class<T>
}