package com.asafonov.outbox.application.event.impl

import com.asafonov.outbox.application.config.CallBlockPolicy
import com.asafonov.outbox.application.event.OutboxSettingsManager
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventTrigger
import com.asafonov.outbox.domain.event.OutboxEventType
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.two.KotlinLogging
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.Environment
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ConcurrentHashMap

open class OutboxSettingsManagerImpl(
      open val context: GenericApplicationContext,
      open val environment: Environment
): OutboxSettingsManager {

    open val logger = KotlinLogging.logger {}

    companion object {
        const val THREAD_PREFIX: String = "-"
    }

    private val coroutineDispatchersMap: MutableMap<String, ExecutorCoroutineDispatcher> =
        ConcurrentHashMap<String, ExecutorCoroutineDispatcher>()

    private val outboxEventTypeSettingsMap: MutableMap<String, OutboxEventSettings> =
        ConcurrentHashMap<String, OutboxEventSettings>()

    private val triggersMap: MutableMap<String, OutboxEventTrigger> =
        ConcurrentHashMap<String, OutboxEventTrigger>()

    override fun shutdownAllTaskExecutors() {
        coroutineDispatchersMap.values.forEach { dispatcher ->
            logger.info { "Shutting down threadPool: $dispatcher" }
            closeDispatcher(dispatcher)
        }
    }

    override fun getDispatcherOrAddIfAbsent(eventType: OutboxEventType): ExecutorCoroutineDispatcher {
        return coroutineDispatchersMap[eventType.getName()] ?:
            addCoroutineDispatcher(eventType.getName(), getSettingOrAddIfAbsent(eventType))
    }

    override fun getSettingOrAddIfAbsent(eventType: OutboxEventType): OutboxEventSettings {
        return outboxEventTypeSettingsMap[eventType.getName()] ?: addOutboxEventSetting(eventType.getName())
    }

    override fun getTriggerOrAddIfAbsent(eventType: OutboxEventType): OutboxEventTrigger {
        return triggersMap[eventType.getName()] ?: addTrigger(eventType)
    }

    @EventListener(RefreshScopeRefreshedEvent::class)
    fun onRefresh(event: RefreshScopeRefreshedEvent) {
        logger.info(" Refresh Event in Outbox Settings: {$event}")
        outboxEventTypeSettingsMap.keys
            .forEach { eventType ->
                val oldSettings = outboxEventTypeSettingsMap[eventType]
                val newSettings = OutboxEventSettings(eventType, environment)
                outboxEventTypeSettingsMap[eventType] = newSettings

                if (oldSettings == null || oldSettings.isThreadPoolPropertyChanged(newSettings)) {
                    
                    val dispatcher = coroutineDispatchersMap[eventType]
                    closeDispatcher(dispatcher)
                    logger.info { "Shutting down OLD dispatcher: $dispatcher" }
                    addCoroutineDispatcher(eventType, newSettings)
                }
            }
    }

    private fun addCoroutineDispatcher(eventType: String, setting: OutboxEventSettings): ExecutorCoroutineDispatcher {
        logger.info("CREATING POOL FOR $eventType")
        val taskExecutor = ThreadPoolTaskExecutor()
        taskExecutor.maxPoolSize = setting.poolMaxSize
        taskExecutor.corePoolSize = setting.poolCoreSize
        taskExecutor.queueCapacity = calculateSize(setting.loadEventsBatch, eventType)
        taskExecutor.setThreadNamePrefix(eventType + THREAD_PREFIX)
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true)
        taskExecutor.setRejectedExecutionHandler(CallBlockPolicy(setting.timeout))
        taskExecutor.setAllowCoreThreadTimeOut(true)
        taskExecutor.initialize()
        coroutineDispatchersMap[eventType] = taskExecutor.asCoroutineDispatcher() as ExecutorCoroutineDispatcher
        context.registerBean("$eventType$THREAD_PREFIX",  ExecutorCoroutineDispatcher::class.java,
            taskExecutor)
        return coroutineDispatchersMap[eventType]!!
    }

    private fun addOutboxEventSetting(eventType: String): OutboxEventSettings {
        outboxEventTypeSettingsMap[eventType] = OutboxEventSettings(eventType, environment)
        logger.info("Adding event settings for $eventType: ${outboxEventTypeSettingsMap[eventType]}")
        return outboxEventTypeSettingsMap[eventType]!!
    }

    private fun addTrigger(eventType: OutboxEventType): OutboxEventTrigger {
        val setting = getSettingOrAddIfAbsent(eventType)
        val trigger = OutboxEventTrigger(setting.repeatDelay)
        triggersMap[eventType.getName()] = trigger
        logger.info { "RepeatDelay Milliseconds for ${eventType.getName()} is ${setting.repeatDelay}" }
        return trigger
    }

    private fun calculateSize(loadEventBatch: Int, eventType: String): Int {
        if (loadEventBatch < 1) {
            throw IllegalStateException("LoadEventBatch is less then 1 for : $eventType")
        }

        if (loadEventBatch < 2) {
            return loadEventBatch
        }

        return loadEventBatch / 2
    }


    private fun closeDispatcher(dispatcher: ExecutorCoroutineDispatcher?) {
        dispatcher!!.close()
        runBlocking {
            withContext(dispatcher) {
                coroutineContext[Job]?.children?.forEach { it.join() }
            }
        }
    }
}