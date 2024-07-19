package com.asafonov.outbox.application.event

import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventTrigger
import com.asafonov.outbox.domain.event.OutboxEventType
import kotlinx.coroutines.ExecutorCoroutineDispatcher

interface OutboxSettingsManager {
    /**
     * Shuts down all ThreadPoolTaskExecutors for all queues.
     */
    fun shutdownAllTaskExecutors()

    /**
     * Retrieves or adds an ExecutorCoroutineDispatcher for the specified queue type.
     *
     * @param eventType The type of queue for which to find or add the dispatcher.
     * @return The ExecutorCoroutineDispatcher associated with the specified queue type.
     */
    fun getDispatcherOrAddIfAbsent(eventType: OutboxEventType): ExecutorCoroutineDispatcher

    /**
     * Retrieves or adds the OutboxEventSettings for the specified queue type.
     *
     * @param eventType The type of queue for which to find or add the settings.
     * @return The OutboxEventSettings associated with the specified queue type.
     */
    fun getSettingOrAddIfAbsent(eventType: OutboxEventType): OutboxEventSettings

    /**
     * Retrieves or adds the OutboxEventTrigger for the specified queue type.
     *
     * @param eventType The type of queue for which to find or add the trigger.
     * @return The OutboxEventTrigger associated with the specified queue type.
     */
    fun getTriggerOrAddIfAbsent(eventType: OutboxEventType): OutboxEventTrigger
}