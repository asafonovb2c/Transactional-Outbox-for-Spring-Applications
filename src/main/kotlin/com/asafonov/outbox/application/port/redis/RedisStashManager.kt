package com.asafonov.outbox.application.port.redis

import com.asafonov.outbox.domain.lock.KeyHolder

/**
 * Manages Redis as a storage system.
 */
interface RedisStashManager {

    /**
     * Retrieves keys that are currently being processed from the stash.
     *
     * @param stashName The name of the stash where keys are stored.
     * @return A collection of keys that are currently being processed.
     */
    fun getFromStash(stashName: String): Collection<KeyHolder>

    /**
     * Adds keys that are currently being processed to the stash with an expiration time.
     *
     * @param stashName The name of the stash where keys will be stored.
     * @param stashKey The key in the stash for locking.
     * @param expiration The expiration time for the keys in milliseconds.
     * @param keys The keys to be stored.
     */
    fun putInStashWithExpiration(stashName: String, stashKey: String, expiration: Long, keys: KeyHolder)

    /**
     * Deletes keys from the stash that are currently being processed.
     *
     * @param stashName The name of the stash where keys are stored.
     * @param stashKey The key in the stash for locking.
     */
    fun deleteFromStash(stashName: String, stashKey: String)
}