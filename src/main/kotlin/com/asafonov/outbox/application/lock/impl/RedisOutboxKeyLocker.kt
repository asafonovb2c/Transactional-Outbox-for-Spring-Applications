package com.asafonov.outbox.application.lock.impl

import com.asafonov.outbox.application.lock.OutboxKeyLocker
import mu.two.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.TimeUnit

open class RedisOutboxKeyLocker(open val redisTemplate: StringRedisTemplate) : OutboxKeyLocker {

    open  val logger = KotlinLogging.logger {}

    override fun tryLockWithTimeOut(lockKey: String, timeoutMilliseconds: Long): Boolean {
        try {
            if (lockKey.isEmpty()) {
                logger.warn { "Got lockKey.isEmpty() while trying to tryLockWithTimeOut for $lockKey"}
                return false
            }
            val result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, timeoutMilliseconds, TimeUnit.MILLISECONDS)
            return result!!
        } catch (e : Exception) {
            logger.error(e) { "Got exception while trying to tryLockWithTimeOut for $lockKey"}
            return false
        }
    }

    override fun unlock(lockKey: String, wasLocked: Boolean?) {
        try {
            if (wasLocked != null && wasLocked) {
                val value = redisTemplate.opsForValue().get(lockKey)
                if (value == lockKey) {
                    redisTemplate.delete(lockKey)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Got exception while trying to unlock for $lockKey"}
            return
        }
    }

    override suspend fun tryLockWithTimeOutAsync(lockKey: String, timeoutMilliseconds: Long): Boolean {
        return tryLockWithTimeOut(lockKey, timeoutMilliseconds)
    }

    override suspend fun unlockAsync(lockKey: String, wasLocked: Boolean) {
        unlock(lockKey, wasLocked)
    }

}