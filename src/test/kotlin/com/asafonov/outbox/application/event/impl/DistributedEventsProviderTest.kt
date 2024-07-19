package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.application.event.impl.DistributedEventsProvider
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.application.port.redis.RedisStashManager
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.TestOutboxEventType
import com.asafonov.outbox.domain.lock.KeyHolder
import com.asafonov.outbox.out.repository.event.createNewEvent
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import java.util.UUID

class DistributedEventsProviderTest {

    private val outboxEventDbPort: OutboxEventDbPort = spyk<OutboxEventDbPort>()
    private val redisStash: RedisStashManager = spyk<RedisStashManager>()
    private val distributedEventsProvider: DistributedEventsProvider = DistributedEventsProvider(outboxEventDbPort, redisStash)
    private val mockEnvironment = MockEnvironment()

    private val eventType = TestOutboxEventType.TEST_EVENT
    private val settings: OutboxEventSettings = OutboxEventSettings(eventType.getName(), mockEnvironment)
    private val keyHolder = KeyHolder(listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        UUID.randomUUID().toString(), 1111)
    private val keyHolders = listOf(keyHolder, keyHolder)
    private val events = listOf(createNewEvent())
    private val sessionUuid = UUID.randomUUID().toString()

    @Test
    @DisplayName("Run All Operations for a Single Queue Type")
    fun getEvents_deleteOldEvents_getNewEvents() {
        every { redisStash.getFromStash(settings.stashName)} returns keyHolders
        every { outboxEventDbPort.selectEnabledEventsWithoutExcluded(eventType,
            settings.attemptsMax, settings.loadEventsBatch, any()) } returns events

        val newEvents = distributedEventsProvider.getEvents(eventType, settings, sessionUuid)

        verify(atLeast = 2) { redisStash.deleteFromStash(settings.stashName, any()) }
        verify { redisStash.putInStashWithExpiration(settings.stashName, sessionUuid, settings.timeout, any())}

        Assertions.assertEquals(newEvents, events)
    }
}