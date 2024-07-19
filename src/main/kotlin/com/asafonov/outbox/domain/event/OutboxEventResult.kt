package com.asafonov.outbox.domain.event

/**
 * Result of processing a queue event.
 * @property isProcessed Indicates whether the event was successfully processed.
 * @property lockKeyIsBusy Indicates that the event was not processed due to the lock key being busy.
 * @property retryReason The reason for the retry.
 */
data class OutboxEventResult(
    val isProcessed: Boolean,
    val lockKeyIsBusy: Boolean,
    var retryReason: String? = null
)

fun createEventProcessed(): OutboxEventResult {
    return OutboxEventResult(isProcessed = true, lockKeyIsBusy = false)
}

fun createEventWithFailedResult(sleepReason: String?): OutboxEventResult {
    return OutboxEventResult(isProcessed = false, lockKeyIsBusy = false, sleepReason)
}

fun createEventLockBusyResult(lockKey: String): OutboxEventResult {
    return OutboxEventResult(isProcessed = false, lockKeyIsBusy = true, "$lockKey was locked")
}

fun createEventFailedWithException(sleepReason: String?, e: Throwable?): OutboxEventResult {
    return OutboxEventResult(isProcessed = false, lockKeyIsBusy = false,
        sleepReason + ( if (e == null) "" else (". " + e.stackTrace) ))
}