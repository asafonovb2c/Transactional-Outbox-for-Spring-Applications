package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.application.event.impl.OutboxSettingsManagerImpl
import com.asafonov.outbox.domain.TestOutboxEventType
import kotlinx.coroutines.isActive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext
import org.springframework.mock.env.MockEnvironment

class OutboxSettingsManagerImplTest {

    private val context: GenericApplicationContext = GenericApplicationContext()
    private val mockEnvironment = MockEnvironment()

    private val settingsManager = OutboxSettingsManagerImpl(context, mockEnvironment)

    private val eventType = TestOutboxEventType.TEST_EVENT

    @Test
    @DisplayName("Прогон всех операций для одного типа очереди")
    fun allOperations_runThrough_becauseItEasier() {
        var dispatcher = settingsManager.getDispatcherOrAddIfAbsent(eventType)
        Assertions.assertNotNull(dispatcher)

        val settings = settingsManager.getSettingOrAddIfAbsent(eventType)
        Assertions.assertNotNull(settings)

        val trigger = settingsManager.getTriggerOrAddIfAbsent(eventType)
        Assertions.assertNotNull(trigger)

        settingsManager.shutdownAllTaskExecutors()
        dispatcher = settingsManager.getDispatcherOrAddIfAbsent(eventType)
        Assertions.assertFalse(dispatcher.isActive)
    }

}