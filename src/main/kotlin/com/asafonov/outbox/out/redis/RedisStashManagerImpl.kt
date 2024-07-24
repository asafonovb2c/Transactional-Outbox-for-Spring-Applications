package com.asafonov.outbox.out.redis

import com.asafonov.outbox.application.event.impl.JacksonSerializer
import com.asafonov.outbox.application.port.redis.RedisStashManager
import com.asafonov.outbox.domain.lock.KeyHolder
import mu.two.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate

open class RedisStashManagerImpl(open val redisTemplate: StringRedisTemplate) : RedisStashManager {

    private val logger = KotlinLogging.logger {}

    override fun getFromStash(stashName: String): Collection<KeyHolder> {
        try {
            return redisTemplate
                .opsForHash<String, String>()
                .values(stashName)
                .map { JacksonSerializer.deserialize(it, KeyHolder::class.java) }
                .toList()
        } catch (e: Exception) {
            logger.error(e) { "Got exception while trying to getFromStash for $stashName" }
            return emptyList()
        }
    }

    override fun putInStashWithExpiration(stashName: String, stashKey: String, expiration: Long, keys: KeyHolder) {
        try {
            if (stashName.isNullOrEmpty() || stashKey.isNullOrEmpty()) {
                logger.warn { "Got null values when putInStashWithExpiration for" +
                        " $stashName, $stashKey, $expiration, keys $keys" }
                return
            }
            redisTemplate.opsForHash<String, String>().put(stashName, stashKey, JacksonSerializer.serialize(keys))
        } catch (e: Exception) {
            logger.error(e) { "Got exception while trying to putInStashWithExpiration for $stashName, $stashKey, " +
                    "$expiration, keys $keys" }
        }
    }

    override fun deleteFromStash(stashName: String, stashKey: String) {
        try {
            if (stashName.isNullOrEmpty() || stashKey.isNullOrEmpty()) {
                logger.warn { "Got null values when deleteFromStash for $stashName, $stashKey" }
                return
            }
            redisTemplate.opsForHash<String, String>().delete(stashName, stashKey)
        } catch (e: Exception) {
            logger.error(e) { "Got exception while trying to deleteFromStash for $stashName, $stashKey" }
        }
    }
}

