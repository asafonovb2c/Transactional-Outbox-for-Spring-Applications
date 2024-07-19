package com.asafonov.outbox.application.outboxEvent.impl

import com.asafonov.outbox.BaseIntegrationTest
import com.asafonov.outbox.application.OutboxEventProcessor
import com.asafonov.outbox.application.event.impl.TestOutboxEventHandleStrategy
import com.asafonov.outbox.out.repository.event.OutboxEventMapper
import com.asafonov.outbox.out.repository.event.createNewEvent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired

class OutboxEventProcessorImplTest: BaseIntegrationTest()  {

    @Autowired
    var outboxEventProcessor: OutboxEventProcessor? = null
    @Autowired
    var outboxEventMapper: OutboxEventMapper? = null

    private val strategy = TestOutboxEventHandleStrategy()

    @Test
    @DisplayName("Проверяем обработку пустой очереди")
    fun process_empty_doesNotThrow() {
        assertDoesNotThrow {
            outboxEventProcessor!!.process(strategy)
        }
    }

    @Test
    @DisplayName("Проверяем обработку очереди с событиями")
    fun process_filled_doesNotThrow() {
        val events = listOf(createNewEvent(),createNewEvent(),createNewEvent(),createNewEvent())
        outboxEventMapper!!.insertAll(events)

        assertDoesNotThrow {
            outboxEventProcessor!!.process(strategy)
        }
    }
}