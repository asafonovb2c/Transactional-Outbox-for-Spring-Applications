package com.asafonov.outbox.application.lock

import com.asafonov.outbox.application.lock.impl.ConcurrentMapService
import com.asafonov.outbox.application.lock.impl.LocalOutboxKeyLocker
import com.asafonov.outbox.application.lock.impl.RedisOutboxKeyLocker
import com.asafonov.outbox.out.redis.RedisStashManagerImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
open class TestRedisConfig {

    @Value("\${spring.redis.database}")
    var database: Int = 0
    @Value("\${spring.redis.host}")
    val address: String = ""
    @Value("\${spring.redis.port}")
    val port: Int = 0

    @Bean
    open fun outboxRedisConnectionFactory(): RedisConnectionFactory {
        val connectionFactory = LettuceConnectionFactory(address, port)
        connectionFactory.database = database
        return connectionFactory
    }

    @Bean
    open fun redisStringTemplate(redisConnectionFactory: RedisConnectionFactory): StringRedisTemplate {
        val template = StringRedisTemplate()
        template.connectionFactory = redisConnectionFactory
        return template
    }

    @Bean
    open fun redisLocker(@Qualifier("redisStringTemplate") redisClient: StringRedisTemplate): RedisOutboxKeyLocker {
        return RedisOutboxKeyLocker(redisClient)
    }

    @Bean
    open fun redisStashManager(@Qualifier("redisStringTemplate") redisClient: StringRedisTemplate): RedisStashManagerImpl {
        return RedisStashManagerImpl(redisClient)
    }

    @Bean
    open fun concurrentMapService(): ConcurrentMapService {
        return ConcurrentMapService()
    }

    @Bean
    open fun localKeyLocker(): LocalOutboxKeyLocker {
        return LocalOutboxKeyLocker()
    }
}