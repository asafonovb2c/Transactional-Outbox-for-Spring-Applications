package com.asafonov.outbox.application.lock

import com.asafonov.outbox.domain.lock.KeyHolder
import com.asafonov.outbox.out.redis.RedisStashManagerImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@TestPropertySource(locations = ["/application.properties"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [TestRedisConfig::class])
class RedisStashManagerImplTest {

    @Autowired
    private lateinit var redisStashManager: RedisStashManagerImpl

    @Test
    @DisplayName("Проверка сета коллекций в редис")
    fun basicLockTest() {
        val stashName = "stashName"

        val stashKey1 = "stashKey1"
        val stashKey2 = "stashKey2"

        val timeout = 6666L

        val keys = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val keyHolder1 = KeyHolder(keys, stashName,  timeout)
        val keyHolder2 = KeyHolder(keys, stashName, timeout)

        redisStashManager.putInStashWithExpiration(stashName, stashKey1, timeout, keyHolder1)
        redisStashManager.putInStashWithExpiration(stashName, stashKey2, timeout, keyHolder2)

        var persistedKeys =  redisStashManager.getFromStash(stashName)
        Assertions.assertEquals(persistedKeys.map { it.keys }.flatten().size, keys.size*2)

        redisStashManager.deleteFromStash(stashName, stashKey1)

        persistedKeys =  redisStashManager.getFromStash(stashName)
        Assertions.assertEquals(persistedKeys.size, 1)
    }
}