package com.asafonov.outbox.out.repository.event

import com.asafonov.outbox.domain.event.ExecutionType
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.TestOutboxEventType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class OutboxEventSettingsTest {


    @Test
    @DisplayName("Creating OutboxEventSettings with empty config")
    fun createFromEmptyEnv_basicOperationsTest() {
        val environment = MockEnvironment()
        val settings = OutboxEventSettings(TestOutboxEventType.TEST_EVENT.getName(), environment)

        Assertions.assertEquals(settings.processEnabled, true)
        Assertions.assertEquals(settings.saveEnabled, true)
        Assertions.assertEquals(settings.loadEventsBatch, 100)
        Assertions.assertEquals(settings.firstSleepDelay, 0)
        Assertions.assertEquals(settings.repeatDelayOnLocked, 1000)
        Assertions.assertEquals(settings.repeatDelay, 0)
        Assertions.assertEquals(settings.repeatDelayOnEmpty, 10000)
        Assertions.assertEquals(settings.nextDelay, 10000)
        Assertions.assertEquals(settings.poolCoreSize, 1)
        Assertions.assertEquals(settings.poolMaxSize, 2)
        Assertions.assertEquals(settings.coroutinesPerThread, 5)
        Assertions.assertEquals(settings.attemptsMax, 3)
        Assertions.assertEquals(settings.timeout, 120000)
        Assertions.assertEquals(settings.executionType, ExecutionType.PARALLEL)
        Assertions.assertEquals(settings.stashName, TestOutboxEventType.TEST_EVENT.getName()+"-HASH")
        Assertions.assertEquals(settings.nextDelayCoefficient, 1f)
        Assertions.assertEquals(settings.nextDelayIncrease, true)
    }
}