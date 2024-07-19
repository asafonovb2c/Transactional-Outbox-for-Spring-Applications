package com.asafonov.outbox.application.event.impl

import com.asafonov.outbox.application.OutboxEventHandleStrategy
import com.asafonov.outbox.application.OutboxEventProcessor
import com.asafonov.outbox.application.event.EventsProvider
import com.asafonov.outbox.application.event.OutboxSettingsManager
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.application.port.redis.RedisStashManager
import com.asafonov.outbox.domain.event.ExecutionType
import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventDto
import com.asafonov.outbox.domain.event.OutboxEventResult
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.out.repository.config.LockType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

open class OutboxEventProcessorImpl(
        open val outboxEventDbPort: OutboxEventDbPort,
        open val coordinator: OutboxEventStrategyCoordinator,
        open val outboxSettingsManager: OutboxSettingsManager,
        open val locker: OutboxKeyLocker,
        open val eventsProvider: EventsProvider,
        open val redisStash: RedisStashManager?
) : OutboxEventProcessor {

    @Value("\${outbox.key.lockType}")
    private val lockType: String? = null
    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun <T : OutboxEventDto> process(strategy: OutboxEventHandleStrategy<T>) {
        val eventType = strategy.getEventType()
        val settings = outboxSettingsManager.getSettingOrAddIfAbsent(eventType)
        val trigger = outboxSettingsManager.getTriggerOrAddIfAbsent(eventType)
        val dispatcher = outboxSettingsManager.getDispatcherOrAddIfAbsent(eventType)

        if (!settings.processEnabled) {
            trigger.setDelay(settings.repeatDelayOnEmpty)
            return
        }

        val locked = locker.tryLockWithTimeOut(eventType.getName(), settings.repeatDelayOnLocked)

        if (!locked) {
            trigger.setDelay(settings.repeatDelayOnLocked)
            return
        }

        val sessionUuid = UUID.randomUUID().toString()
        val eventsToDelete: MutableList<OutboxEvent> = CopyOnWriteArrayList()
        val eventsToUpdate: MutableList<OutboxEvent> = CopyOnWriteArrayList()

        try {
            val events: Collection<OutboxEvent> = eventsProvider.getEvents(eventType, settings, sessionUuid)

            if (settings.executionType != ExecutionType.EXCLUSIVE) {
                locker.unlock(eventType.getName(), locked)
            }

            if (events.isEmpty()) {
                trigger.setDelay(settings.repeatDelayOnEmpty)
                logger.debug("No events was found for ${eventType.getName()}")
                return
            }

            runBlocking {
                val deferredEvents = events.map { event ->
                    async(dispatcher.limitedParallelism(settings.poolMaxSize * settings.coroutinesPerThread)) {
                        try {
                            val result = coordinator.applyEventStrategy(strategy, event, settings)
                            groupEvents(result, event, settings, eventsToDelete, eventsToUpdate)
                        } catch (e: Exception) {
                            logger.error(e) {"Got Exception while handling event: {$event}"}
                            event.failReason = "Got Exception while handling event: ${e.stackTraceToString()}"
                            event.status = OutboxEventStatus.DISABLED
                            eventsToUpdate.add(event)
                        }
                    }
                }

                deferredEvents.awaitAll()
            }

            trigger.setDelay(settings.repeatDelay)
        } catch (e: Exception) {
            logger.error(e) {"Got Exception while handling event: ${eventType.getName()}"}
            throw e
        } finally {
            outboxEventDbPort.delete(eventsToDelete)
            outboxEventDbPort.update(eventsToUpdate)

            if (lockType.equals(LockType.REDIS.name)) {
                redisStash!!.deleteFromStash(eventType.getName(), sessionUuid)
            }
            locker.unlock(eventType.getName(), locked)
        }
    }

    private fun groupEvents(result: OutboxEventResult, event: OutboxEvent, settings: OutboxEventSettings,
                            eventsToDelete: MutableList<OutboxEvent>, eventsToUpdate: MutableList<OutboxEvent>) {
        if (result.isProcessed) {
            eventsToDelete.add(event)
            return
        }

        event.failReason = result.retryReason
        event.runTime = settings.getNewNextTime(event.attempts)
        event.status = OutboxEventStatus.ENABLED

        if (result.lockKeyIsBusy) {
            eventsToUpdate.add(event)
            return
        }

        event.attempts += 1

        if (settings.deleteAfterAttempts && (event.attempts > settings.attemptsMax)) {
            eventsToDelete.add(event)
            return
        }

        eventsToUpdate.add(event)
    }
}