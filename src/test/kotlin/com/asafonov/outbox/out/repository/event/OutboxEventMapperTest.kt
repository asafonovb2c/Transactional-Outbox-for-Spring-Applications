package com.asafonov.outbox.out.repository.event

import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.domain.TestOutboxEventType
import com.asafonov.outbox.out.repository.BaseDaoTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

class OutboxEventMapperTest : BaseDaoTest() {

    @Autowired
    lateinit var mapper: OutboxEventMapper

    @Test
    @DisplayName("CRUD operations test")
    fun basicOperationsTest() {
        val event = createNewEvent()
        mapper.insert(event)

        var persistedEvents = mapper.selectEvents(
            TestOutboxEventType.TEST_EVENT.name, OutboxEventStatus.ENABLED,
            Instant.now().plusSeconds(1), 1, 100)

        val persistedEvent = persistedEvents.iterator().next()

        Assertions.assertEquals(event.eventType, persistedEvent.eventType)

        val excludedUuids = MutableList(2000) { UUID.randomUUID().toString() }
        excludedUuids.add(persistedEvent.uuid)
        persistedEvents = mapper.selectEventsWithoutUuids(
            TestOutboxEventType.TEST_EVENT.name, OutboxEventStatus.ENABLED,
            Instant.now().plusSeconds(1), 1, 100, excludedUuids
        )

        Assertions.assertTrue(persistedEvents.isEmpty())

        var size = mapper.selectCountEvents()
        Assertions.assertEquals(size.iterator().next().size, 1)

        event.uuid = persistedEvent.uuid
        event.attempts = 2
        mapper.updateEvents(listOf(event))

        persistedEvents = mapper.selectEvents(
            TestOutboxEventType.TEST_EVENT.name, OutboxEventStatus.ENABLED,
            Instant.now().plusSeconds(1), 1, 100)

        Assertions.assertTrue(persistedEvents.isEmpty())

        val event2 = createNewEvent()
        val event3 = createNewEvent()
        val event4 = createNewEvent()

        mapper.insertAll(listOf(event4,event2,event3))

        size = mapper.selectCountEvents()
        Assertions.assertEquals(size.iterator().next().size, 4)

        val newEvents = mapper.selectEvents(
            TestOutboxEventType.TEST_EVENT.name, OutboxEventStatus.ENABLED,
            Instant.now().plusSeconds(1), 3, 100)

        mapper.delete(newEvents)

        size = mapper.selectCountEvents()
        Assertions.assertTrue(size.isEmpty())
    }


}