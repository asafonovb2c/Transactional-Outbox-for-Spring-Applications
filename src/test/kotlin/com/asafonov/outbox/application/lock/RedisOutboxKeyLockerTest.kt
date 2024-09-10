
import com.asafonov.outbox.application.lock.OutboxKeyLocker
import com.asafonov.outbox.application.lock.TestRedisConfig
import com.asafonov.outbox.application.lock.impl.RedisOutboxKeyLocker
import com.asafonov.outbox.out.repository.AdditionalDaoConfig
import com.asafonov.outbox.out.repository.config.FlywayOutboxConfiguration
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

@TestPropertySource(locations = ["/application.properties"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [TestRedisConfig::class, FlywayOutboxConfiguration::class, AdditionalDaoConfig::class])
class RedisOutboxKeyLockerTest {

    @Autowired
    var locker: RedisOutboxKeyLocker? = null

    val key1 = "Key1"
    val keyAs1 = "Key1"
    val key2 = "Key2"


    @OptIn(DelicateCoroutinesApi::class)
    @Test
    @DisplayName("Verification of Redis Locks Functionality")
    fun redisLockTest_blockValue_unlockResult() {
        val keys = listOf(key1, keyAs1, key2)
        val repeatedKeys = keys.flatMap { key -> List(100) { key } }
        val listOfTrue = CopyOnWriteArrayList<Boolean>()
        val listOfFalse = CopyOnWriteArrayList<Boolean>()

        Assertions.assertEquals(repeatedKeys.size, 300)

        runBlocking {
            val deferredEvents = repeatedKeys.map { event ->
                async(newSingleThreadContext("Thread1")) {
                    val result = workSimulation(event, locker)
                    if (result) listOfTrue.add(result)
                    else listOfFalse.add(result)
                }
            }

            deferredEvents.awaitAll()
        }

        Assertions.assertEquals(listOfTrue.size, 2)
        Assertions.assertEquals(listOfFalse.size, 298)

        Assertions.assertTrue(locker!!.tryLockWithTimeOut(key1, 1000))
        Assertions.assertFalse(locker!!.tryLockWithTimeOut(keyAs1, 1000))
        Assertions.assertTrue(locker!!.tryLockWithTimeOut(key2, 1000))
    }

    suspend fun workSimulation(key: String, locker: OutboxKeyLocker?): Boolean {
        if (locker!!.tryLockWithTimeOutAsync(key, 20000L)) {
            withContext(Dispatchers.IO) {
                Thread.sleep(8000)
            }
            println(key + Instant.now().toEpochMilli())
            locker.unlockAsync(key, true)
            return true
        }

        return false
    }
}