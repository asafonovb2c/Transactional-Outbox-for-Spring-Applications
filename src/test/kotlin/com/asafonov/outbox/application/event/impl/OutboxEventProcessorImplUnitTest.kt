package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.application.event.EventsProvider
import com.asafonov.outbox.application.event.OutboxSettingsManager
import com.asafonov.outbox.application.event.impl.DistributedEventsProvider
import com.asafonov.outbox.application.event.impl.OutboxEventProcessorImpl
import com.asafonov.outbox.application.event.impl.OutboxEventStrategyCoordinator
import com.asafonov.outbox.application.event.impl.TestOutboxEventHandleStrategy
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.application.port.redis.RedisStashManager
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventTrigger
import com.asafonov.outbox.domain.TestOutboxEventType
import com.asafonov.outbox.domain.event.createEventProcessed
import com.asafonov.outbox.out.repository.event.createNewEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.integration.util.CallerBlocksPolicy
import org.springframework.mock.env.MockEnvironment
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

class OutboxEventProcessorImplUnitTest {

    private val outboxEventDbPort: OutboxEventDbPort = spyk<OutboxEventDbPort>()
    private val outboxSettingsManager: OutboxSettingsManager = spyk<OutboxSettingsManager>()
    private val eventsProvider: EventsProvider = mockk<DistributedEventsProvider>()
    private val redisStash: RedisStashManager = spyk<RedisStashManager>()
    private val locker: OutboxKeyLocker = spyk<OutboxKeyLocker>()
    private val coordinator: OutboxEventStrategyCoordinator = mockk<OutboxEventStrategyCoordinator>()

    @InjectMockKs
    private val outboxEventProcessor: OutboxEventProcessorImpl = OutboxEventProcessorImpl(outboxEventDbPort, coordinator,
        outboxSettingsManager, locker, eventsProvider, redisStash)

    private val strategy = TestOutboxEventHandleStrategy()
    private val eventType = TestOutboxEventType.TEST_EVENT
    private val mockEnvironment = MockEnvironment()
    private val settings: OutboxEventSettings = OutboxEventSettings(eventType.getName(), mockEnvironment)

    private val events = MutableList(1000) { createNewEvent() }

    @Test
    @DisplayName("Проверка работоспособности обработки событий")
    fun process_filled_deleteAllEvents() {
        val taskExecutor = ThreadPoolTaskExecutor()
        taskExecutor.maxPoolSize = settings.poolMaxSize
        taskExecutor.corePoolSize = settings.poolCoreSize
        taskExecutor.queueCapacity = settings.poolMaxSize
        taskExecutor.setThreadNamePrefix(eventType.getName())
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true)
        taskExecutor.setRejectedExecutionHandler(CallerBlocksPolicy(1200000))
        taskExecutor.setAllowCoreThreadTimeOut(true)
        taskExecutor.initialize()
        val dispatcher = taskExecutor.asCoroutineDispatcher() as ExecutorCoroutineDispatcher
        val trigger = OutboxEventTrigger(settings.repeatDelay)

        every { outboxSettingsManager.getTriggerOrAddIfAbsent(eventType)} returns trigger
        every { outboxSettingsManager.getDispatcherOrAddIfAbsent(eventType)} returns dispatcher
        every { outboxSettingsManager.getSettingOrAddIfAbsent(eventType)} returns settings
        every { eventsProvider.getEvents(eventType, settings, any()) } returns events
        every { locker.tryLockWithTimeOut(eventType.getName(), any()) } returns true
        coEvery {  coordinator.applyEventStrategy(strategy, any(), settings) } returns createEventProcessed()

        outboxEventProcessor.process(strategy)

        verify { outboxEventDbPort.delete( match {it.size == events.size}) }
        verify(exactly = 0) { redisStash.deleteFromStash(eventType.getName(), any())}
        verify { locker.unlock(eventType.getName(), any()) }
    }

}