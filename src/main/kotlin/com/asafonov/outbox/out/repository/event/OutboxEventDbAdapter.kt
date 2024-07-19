package com.asafonov.outbox.out.repository.event

import com.asafonov.outbox.application.event.OutboxSettingsManager
import com.asafonov.outbox.application.event.impl.OutboxEventFactory
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventDto
import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.domain.event.OutboxEventType
import com.asafonov.outbox.domain.metric.OutboxEventTypeMetricsDto
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.stream.Collectors

open class OutboxEventDbAdapter(
        open val outboxEventFactory: OutboxEventFactory,
        open val settingsManager: OutboxSettingsManager
) : OutboxEventDbPort {

    @set: Autowired
    lateinit var outboxEventDAO: OutboxEventMapper

    open  val logger = KotlinLogging.logger {}
    val chunkSize: Int = 1000

    @Transactional
    override fun delete(events: List<OutboxEvent>) {
        if (events.isEmpty()) {
            return
        }
        try {
            val chunks = events.chunked(chunkSize)
            chunks.forEach { c -> outboxEventDAO.delete(c) }
        } catch (e: Throwable) {
            logger.error("It's impossible to remove outboxEvent: $events", e)
            throw e
        }
    }

    @Transactional
    override fun saveNewEvents(type: OutboxEventType, runtime: Instant, events: Set<OutboxEventDto>) {
        try {
            if (!settingsManager.getSettingOrAddIfAbsent(type).saveEnabled) {
                return
            }

            val outboxEvents = events.stream()
                .map { e: OutboxEventDto -> outboxEventFactory.createNewEvent(type, e, runtime)}
                .collect(Collectors.toList())

            val chunks = outboxEvents.chunked(chunkSize)
            chunks.forEach { c -> outboxEventDAO.insertAll(c)}
        } catch (e: Throwable) {
            logger.error("It's impossible to insert outboxEvents: $events", e)
            throw e
        }
    }

    @Transactional
    override fun update(events: List<OutboxEvent>) {
        try {
            val chunks = events.chunked(chunkSize)
            chunks.forEach(outboxEventDAO::updateEvents)
        } catch (e: Throwable) {
            logger.error("It's impossible to update outboxEvent: $events", e)
            throw e
        }
    }

    @Transactional
    override fun selectEnabledEvents(eventType: OutboxEventType, attemptsMax: Long, limit: Int): Collection<OutboxEvent> {
        try {
            return outboxEventDAO.selectEvents(
                eventType.getName(),
                OutboxEventStatus.ENABLED,
                Instant.now(),
                attemptsMax,
                limit
            )
        } catch (e: Throwable) {
            logger.error("Selection error for Collection<outboxEvent>", e)
            throw e
        }
    }

    @Transactional
    override fun selectEnabledEventsWithoutExcluded(eventType: OutboxEventType, attemptsMax: Long, limit: Int,
                                                    excludedUuids: Collection<String>): Collection<OutboxEvent> {
        try {
            return outboxEventDAO.selectEventsWithoutUuids(
                eventType.getName(),
                OutboxEventStatus.ENABLED,
                Instant.now(),
                attemptsMax,
                limit,
                excludedUuids
            )
        } catch (e: Throwable) {
            logger.error("Selection error for Collection<outboxEvent>", e)
            throw e
        }
    }

    @Transactional
    override fun saveNewEvent(eventType: OutboxEventType, eventDto: OutboxEventDto, runtime: Instant) {
        if (!settingsManager.getSettingOrAddIfAbsent(eventType).saveEnabled) {
            return
        }
        val outboxEvent = outboxEventFactory.createNewEvent(eventType, eventDto,
            runtime.plusMillis(settingsManager.getSettingOrAddIfAbsent(eventType).firstSleepDelay))
        outboxEventDAO.insert(outboxEvent)
    }

    @Transactional
    override fun getOutboxEventMetrics(): List<OutboxEventTypeMetricsDto> {
        try {
            return outboxEventDAO.selectCountEvents()
        } catch (e: Throwable) {
            logger.error("Selection error for Collection<outboxEvent>", e)
            throw e
        }
    }
}