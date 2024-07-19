package com.asafonov.outbox.application.lock.impl

import com.asafonov.outbox.application.lock.OutboxKeyLocker
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

open class LocalOutboxKeyLocker(): OutboxKeyLocker {

    open  val logger = KotlinLogging.logger {}
    private val concurrentMapService = ConcurrentMapService()

    override fun tryLockWithTimeOut(lockKey: String, timeoutMilliseconds: Long): Boolean {
        return if (lockKey.isEmpty()) {
            false
        } else {
            runBlocking {
                concurrentMapService.putIfAbsentOrExpired(lockKey, timeoutMilliseconds)
            }
        }
    }

    override fun unlock(lockKey: String, wasLocked: Boolean?) {
        if (wasLocked != null && wasLocked) runBlocking {
            concurrentMapService.remove(lockKey)
        }
    }

    override suspend fun tryLockWithTimeOutAsync(lockKey: String, timeoutMilliseconds: Long): Boolean {
        return if (lockKey.isEmpty()) {
            false
        } else {
            concurrentMapService.putIfAbsentOrExpired(lockKey, timeoutMilliseconds)
        }
    }

    override suspend fun unlockAsync(lockKey: String, wasLocked: Boolean) {
        if (wasLocked) concurrentMapService.remove(lockKey)
    }
}