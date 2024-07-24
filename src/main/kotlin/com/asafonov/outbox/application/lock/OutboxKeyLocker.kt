package com.asafonov.outbox.application.lock

/**
 * Base interface for implementing locks based on a string key.
 */
interface OutboxKeyLocker {
    /**
     * Attempts to acquire a lock using a string key.
     * @param lockKey The key used to acquire the lock.
     * @param timeoutMilliseconds The timeout duration in milliseconds.
     * @return True if the lock was successfully acquired, false otherwise.
     */
    fun tryLockWithTimeOut(lockKey: String, timeoutMilliseconds: Long): Boolean

    /**
     * Releases a lock using a string key.
     * @param lockKey The key used to release the lock.
     * @param wasLocked Indicates whether the lock was previously acquired.
     */
    fun unlock(lockKey: String, wasLocked: Boolean?)

    /**
     * Attempts to acquire a lock using a string key.
     * This version is designed for use in coroutines.
     * @param lockKey The key used to acquire the lock.
     * @param timeoutMilliseconds The timeout duration in milliseconds.
     * @return True if the lock was successfully acquired, false otherwise.
     */
    suspend fun tryLockWithTimeOutAsync(lockKey: String, timeoutMilliseconds: Long): Boolean

    /**
     * Releases a lock using a string key.
     * This version is designed for use in coroutines.
     * @param lockKey The key used to release the lock.
     * @param wasLocked Indicates whether the lock was previously acquired.
     */
    suspend fun unlockAsync(lockKey: String, wasLocked: Boolean)
}