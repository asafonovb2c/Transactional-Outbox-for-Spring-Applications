package com.asafonov.outbox.domain.lock

import kotlinx.coroutines.CompletableDeferred

/**
 * Operations for the lock map.
 */
sealed class MapOperation {
    /**
     * Add to the map if there is no value or if the timeout has expired.
     *
     * @property key The lock key.
     * @property timeOut The desired lock duration in milliseconds.
     * @property now The current timestamp.
     * @property response The response from the map indicating the success of the lock operation.
     */
    data class PutIfAbsent(val key: String, val timeOut: Long, val now: Long, val response: CompletableDeferred<Boolean>) :
        MapOperation()

    /**
     * Remove from the map.
     *
     * @property key The lock key.
     * @property response The response channel within the map.
     */
    data class Remove(val key: String, val response: CompletableDeferred<Boolean>) : MapOperation()
}