package com.asafonov.outbox.domain.event

import org.springframework.scheduling.Trigger
import org.springframework.scheduling.TriggerContext
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

/**
 * Configuration for the scheduler for each queue.
 * @property delay The delay between retries for processing the event, in milliseconds.
 */
class OutboxEventTrigger(delay: Long) : Trigger {

    private var delay: AtomicLong = AtomicLong(delay)

    override fun nextExecutionTime(triggerContext: TriggerContext): Date {
        val lastDate = triggerContext.lastScheduledExecutionTime()
        val last = lastDate?.time ?: System.currentTimeMillis()
        return Date(last + delay.get())
    }

    fun setDelay(newDelay: Long) {
        delay.set(newDelay)
    }
}