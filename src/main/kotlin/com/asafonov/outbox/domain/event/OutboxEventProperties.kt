package com.asafonov.outbox.domain.event

/**
 * Properties of the outbox_event queue.
 *
 * Each event type can have its own parameter for any property,
 * formatted as: outbox.EVENT_TYPE.propertyName.
 */
object OutboxEventProperties {
    
    private const val TEMPLATE_PREFIX: String = "outbox."
    const val PROCESS_ENABLED: String = "outbox.process.enabled"
    const val SAVE_ENABLED: String = "outbox.save.enabled"
    const val LOAD_EVENTS_BATCH: String = "outbox.load.events.batch"
    const val FIRST_SLEEP_DELAY: String = "outbox.first.sleep"
    const val REPEAT_DELAY: String = "outbox.repeat.delay"
    const val REPEAT_DELAY_ON_LOCK: String = "outbox.repeat.delay.on.lock"
    const val REPEAT_DELAY_ON_EMPTY: String = "outbox.repeat.delay.on.empty"
    const val NEXT_DELAY: String = "outbox.next.delay"
    const val NEXT_DELAY_COEFFICIENT: String = "outbox.next.delay.coefficient"
    const val NEXT_DELAY_INCREASE: String = "outbox.next.delay.increase"
    const val ATTEMPTS_MAX: String = "outbox.attempts.max"
    const val DELETE_ATTEMPTS_MAX: String = "outbox.delete.attempts.max"
    const val POOL_MAX_SIZE: String = "outbox.pool.max.size"
    const val POOL_CORE_SIZE: String = "outbox.pool.core.size"
    const val COROUTINES_PER_THREAD: String = "outbox.coroutines.per.thread"
    const val TIMEOUT: String = "outbox.pool.timeout"
    const val EXECUTION_TYPE: String = "outbox.execution.type"
    const val STASH_NAME: String = "outbox.stash.name"

    /**
     * Get properties for a specific queue
     */
    fun getOutboxTypeProperty(propertyName: String, outboxType: String): String {
        return propertyName.replace(TEMPLATE_PREFIX, (TEMPLATE_PREFIX + outboxType) + ".")
    }
}

