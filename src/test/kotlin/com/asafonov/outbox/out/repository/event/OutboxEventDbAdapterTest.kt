package com.asafonov.outbox.out.repository.event

import com.asafonov.outbox.application.event.OutboxSettingsManager
import com.asafonov.outbox.application.event.impl.OutboxEventFactory
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.domain.TestEventDto
import com.asafonov.outbox.domain.TestOutboxEventType
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class OutboxEventDbAdapterTest {
    private var outboxEventDAO: OutboxEventMapper = spyk<OutboxEventMapper>()
    private var outboxEventFactory: OutboxEventFactory = mockk<OutboxEventFactory>()
    private var outboxSettingsManager: OutboxSettingsManager = mockk<OutboxSettingsManager>()
    private val mockEnvironment = MockEnvironment()

    @InjectMockKs
    private val adapter: OutboxEventDbAdapter = OutboxEventDbAdapter(outboxEventFactory, outboxSettingsManager)

    private val outboxEvent = createNewEvent()
    private val outboxEvents = listOf(outboxEvent)
    private val event = TestEventDto("uuid")
    private val type = TestOutboxEventType.TEST_EVENT
    private val now = Instant.now()

    @BeforeEach
    fun setup() {
        ReflectionTestUtils.setField(adapter, "outboxEventDAO", outboxEventDAO);
    }

    @Test
    @DisplayName("Check if the outboxEvents was deleted")
    fun verifyDeleteIsInvoked() {
        adapter.delete(outboxEvents)
        verify { outboxEventDAO.delete(any()) }
    }

    @Test
    @DisplayName("Check if the outboxEvents was saved")
    fun verifySavedNewEvents() {
        every { outboxEventFactory.createNewEvent(type, event, any())} returns outboxEvent
        every { outboxSettingsManager.getSettingOrAddIfAbsent(type)} returns OutboxEventSettings(type.getName(), mockEnvironment)
        outboxSettingsManager.getSettingOrAddIfAbsent(TestOutboxEventType.TEST_EVENT).saveEnabled = true

        adapter.saveNewEvents(type, now, setOf(event))

        verify { outboxEventDAO.insertAll(any()) }
    }

    @Test
    @DisplayName("Check if outboxEvents was updated")
    fun verifyUpdateCalled() {
        adapter.update(outboxEvents)
        verify { outboxEventDAO.updateEvents(any()) }
    }

    @Test
    @DisplayName("Check if outboxEvents was selected")
    fun verifySelectEnabledEvents() {
        every { outboxEventDAO.selectEvents(type.getName(), OutboxEventStatus.ENABLED,
            any(), 2, 2)} returns outboxEvents;

        val result = adapter.selectEnabledEvents(type,2,2)
        assertFalse(result.isEmpty())
    }

    @Test
    @DisplayName("Check if new outboxEvent was saved")
    fun verifySaveNewEvent() {
        every { outboxEventFactory.createNewEvent(type, event, any())} returns outboxEvent
        every { outboxSettingsManager.getSettingOrAddIfAbsent(type)} returns OutboxEventSettings(type.getName(), mockEnvironment)
        outboxSettingsManager.getSettingOrAddIfAbsent(TestOutboxEventType.TEST_EVENT).saveEnabled = true

        adapter.saveNewEvent(type, event, now)

        verify { outboxEventDAO.insert(outboxEvent)}
    }

    @Test
    @DisplayName("Check if new outboxEvent was saved")
    fun getOutboxEventMetrics() {
        adapter.getOutboxEventMetrics()
        return verify { outboxEventDAO.selectCountEvents() }
    }
}