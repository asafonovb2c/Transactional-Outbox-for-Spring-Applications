package com.asafonov.outbox.application.event.impl

import com.asafonov.outbox.application.event.EventsProvider
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventType

open class SingleInstanceEventsProvider(open val outboxEventDbPort: OutboxEventDbPort): EventsProvider {

    override fun getEvents(eventType: OutboxEventType, settings: OutboxEventSettings,
                           sessionUuid: String): Collection<OutboxEvent> {
       return outboxEventDbPort.selectEnabledEvents(eventType, settings.attemptsMax, settings.loadEventsBatch)
    }

}