package com.asafonov.outbox.application.lock.impl

import com.asafonov.outbox.domain.lock.MapOperation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.two.KotlinLogging
import java.time.Instant


open class ConcurrentMapService {

    val channel = Channel<MapOperation>(Channel.UNLIMITED)
    val map = mutableMapOf<String, Long>()
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job)
    open  val logger = KotlinLogging.logger {}

    init { lunchScope() }

    private fun lunchScope() {
        scope.launch {
            for (msg in channel) {
                when (msg) {
                    is MapOperation.PutIfAbsent -> {
                        val response = if (map.containsKey(msg.key) && map[msg.key]!! > msg.now) {
                            false
                        } else {
                            map[msg.key] = msg.timeOut + msg.now
                            true
                        }
                        msg.response.complete(response)
                    }
                    is MapOperation.Remove -> {
                        val response = map.remove(msg.key) != null
                        msg.response.complete(response)
                    }
                }
            }
        }.invokeOnCompletion { throwable ->
            if (throwable != null) {
                logger.error(throwable) { "ConcurrentMapService Channel processing coroutine completed with an error" }
            }
        }
    }

    suspend fun putIfAbsentOrExpired(key: String, value: Long): Boolean {
        val response = CompletableDeferred<Boolean>()
        channel.send(MapOperation.PutIfAbsent(key, value, Instant.now().toEpochMilli(), response))
        return response.await()
    }

    suspend fun remove(key: String): Boolean {
        val response = CompletableDeferred<Boolean>()
        channel.send(MapOperation.Remove(key, response))
        return response.await()
    }
}