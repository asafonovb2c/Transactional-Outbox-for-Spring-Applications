package com.asafonov.outbox.application.event.impl

import com.asafonov.outbox.application.event.EventsProvider
import com.asafonov.outbox.application.port.out.OutboxEventDbPort
import com.asafonov.outbox.application.port.redis.RedisStashManager
import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventSettings
import com.asafonov.outbox.domain.event.OutboxEventType
import com.asafonov.outbox.domain.lock.KeyHolder
import java.time.Instant

open class DistributedEventsProvider(
        open val outboxEventDbPort: OutboxEventDbPort,
        open val redisStash: RedisStashManager
) : EventsProvider {

    override fun getEvents(eventType: OutboxEventType, settings: OutboxEventSettings,
                           sessionUuid: String): Collection<OutboxEvent> {

        val now = Instant.now().toEpochMilli()
        val prosesEvents = redisStash.getFromStash(settings.stashName).groupBy { it.ttl > now }

        val excludedKeys = prosesEvents[true]?.map { it.keys }?.flatten() ?: emptyList()
        val keysHoldersToDelete = prosesEvents[false] ?: emptyList()

        val events = outboxEventDbPort.selectEnabledEventsWithoutExcluded(eventType,
                settings.attemptsMax, settings.loadEventsBatch, excludedKeys)

        keysHoldersToDelete.forEach { redisStash.deleteFromStash(settings.stashName, it.stashKey) }

        redisStash.putInStashWithExpiration(settings.stashName, sessionUuid, settings.timeout,
            KeyHolder(events.map { it.uuid }.toList(), sessionUuid, now + settings.timeout)
        )

        return events
    }
}