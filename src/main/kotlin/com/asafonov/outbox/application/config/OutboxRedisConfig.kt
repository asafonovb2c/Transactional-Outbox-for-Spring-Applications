package com.asafonov.outbox.application.config

import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.application.lock.impl.RedisOutboxKeyLocker
import com.asafonov.outbox.application.port.redis.RedisStashManager
import com.asafonov.outbox.out.redis.RedisStashManagerImpl
import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import mu.two.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@Configuration
@ConditionalOnProperty(name = ["outbox.key.lockType"], havingValue = "REDIS")
open class OutboxRedisConfig {

    @Value("\${spring.redis.database:#{1}}")
    private var database: Int = 1

    @Value("\${spring.redis.host:localhost}")
    private val host: String = "localhost"

    @Value("\${spring.redis.port:1}")
    private val port: String = "1"

    @Value("\${spring.redis.timeout:#{10000}}")
    private val socketTimeout: Long = 10000

    @Value("\${spring.redis.password:}")
    private val password: String? = null

    @Value("\${spring.redis.username:}")
    private val userName: String? = null

    open val logger = KotlinLogging.logger {}

    @Bean("outboxRedisConnectionFactory")
    @ConditionalOnProperty(
        value=["outbox.key.lockType"],
        havingValue = "REDIS",
        matchIfMissing = false
    )
    open fun outboxRedisConnectionFactory(): RedisConnectionFactory {
        val socketOptions = SocketOptions.builder().connectTimeout(Duration.ofMillis(socketTimeout)).build();
        val clientOptions = ClientOptions.builder().socketOptions(socketOptions).build()

        val clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(socketTimeout))
            .clientOptions(clientOptions).build()

        val serverConfig = RedisStandaloneConfiguration()
        serverConfig.database = database
        serverConfig.port = port.toInt()
        serverConfig.hostName = host

        if (!password.isNullOrEmpty()) {
            serverConfig.password =  RedisPassword.of(password)
            serverConfig.username = userName
        }

        val connectionFactory = LettuceConnectionFactory(serverConfig, clientConfig)
        connectionFactory.validateConnection = true

        logger.info { "Connecting to ${serverConfig.hostName}" }
        return connectionFactory
    }


    @Bean
    @ConditionalOnProperty(
        value = ["outbox.key.lockType"],
        havingValue = "REDIS",
        matchIfMissing = false
    )
    open fun outboxStringRedisTemplate(@Qualifier("outboxRedisConnectionFactory")
                                       lettuceConnectionFactory: RedisConnectionFactory):
            StringRedisTemplate {
        val template = StringRedisTemplate()
        template.connectionFactory = lettuceConnectionFactory
        template.afterPropertiesSet();
        return template
    }

    @Bean
    @ConditionalOnProperty(
        value = ["outbox.key.lockType"],
        havingValue = "REDIS",
        matchIfMissing = false
    )
    open fun outboxRedisStashManager(@Qualifier("outboxStringRedisTemplate") redisTemplate: StringRedisTemplate):
            RedisStashManager {
        return RedisStashManagerImpl(redisTemplate)
    }

    @Bean
    @ConditionalOnProperty(
        value = ["outbox.key.lockType"],
        havingValue = "REDIS",
        matchIfMissing = false
    )
    open fun outboxKeyLocker(@Qualifier("outboxStringRedisTemplate") redisTemplate: StringRedisTemplate?):
            OutboxKeyLocker {
        logger.info("Outbox Lock type = REDIS")
        return RedisOutboxKeyLocker(redisTemplate!!)
    }
}