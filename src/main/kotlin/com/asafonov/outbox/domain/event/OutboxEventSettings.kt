package com.asafonov.outbox.domain.event

import com.asafonov.outbox.domain.event.OutboxEventProperties.ATTEMPTS_MAX
import com.asafonov.outbox.domain.event.OutboxEventProperties.COROUTINES_PER_THREAD
import com.asafonov.outbox.domain.event.OutboxEventProperties.DELETE_ATTEMPTS_MAX
import com.asafonov.outbox.domain.event.OutboxEventProperties.EXECUTION_TYPE
import com.asafonov.outbox.domain.event.OutboxEventProperties.FIRST_SLEEP_DELAY
import com.asafonov.outbox.domain.event.OutboxEventProperties.LOAD_EVENTS_BATCH
import com.asafonov.outbox.domain.event.OutboxEventProperties.NEXT_DELAY
import com.asafonov.outbox.domain.event.OutboxEventProperties.NEXT_DELAY_COEFFICIENT
import com.asafonov.outbox.domain.event.OutboxEventProperties.NEXT_DELAY_INCREASE
import com.asafonov.outbox.domain.event.OutboxEventProperties.POOL_CORE_SIZE
import com.asafonov.outbox.domain.event.OutboxEventProperties.POOL_MAX_SIZE
import com.asafonov.outbox.domain.event.OutboxEventProperties.PROCESS_ENABLED
import com.asafonov.outbox.domain.event.OutboxEventProperties.REPEAT_DELAY
import com.asafonov.outbox.domain.event.OutboxEventProperties.REPEAT_DELAY_ON_EMPTY
import com.asafonov.outbox.domain.event.OutboxEventProperties.REPEAT_DELAY_ON_LOCK
import com.asafonov.outbox.domain.event.OutboxEventProperties.SAVE_ENABLED
import com.asafonov.outbox.domain.event.OutboxEventProperties.STASH_NAME
import com.asafonov.outbox.domain.event.OutboxEventProperties.TIMEOUT
import org.springframework.core.env.Environment
import java.time.Instant

/**
 * Configuration for the outbox queue.
 * @property processEnabled Enables or disables processing of events from the database queue.
 * @property saveEnabled Enables or disables saving events to the database.
 * @property loadEventsBatch The limit for the SELECT query from the queue table for subsequent processing.
 * @property firstSleepDelay The initial wait time for processing the first event, in milliseconds.
 * @property repeatDelayOnLocked The wait time if acquiring the lock fails.
 * @property repeatDelay The wait time, in milliseconds, between subsequent attempts to process the queue.
 * @property repeatDelayOnEmpty The wait time, in milliseconds, between subsequent attempts to process the queue if it is empty.
 * @property nextDelay The time in milliseconds after which reprocessing can begin following a failed attempt.
 * @property nextDelayCoefficient The coefficient by which the wait time for the next attempt will be increased.
 * @property nextDelayIncrease Indicates whether to use the nextDelayCoefficient to increase nextDelay for subsequent attempts.
 * @property poolMaxSize The maximum number of threads allocated for the event.
 * @property poolCoreSize The minimum number of threads allocated for the event.
 * @property coroutinesPerThread The number of coroutines per thread.
 * @property attemptsMax The maximum number of attempts after which processing stops.
 * @property deleteAfterAttempts Indicates whether to delete events from the queue after reaching the maximum number of attempts.
 * @property timeout The maximum wait time for processing an event, in milliseconds.
 * @property executionType The method of processing events when multiple instances of the application are present (EXCLUSIVE / PARALLEL).
 * @property stashName The name of the hash for storing UUIDs of events in Redis that are being processed.
 */
class OutboxEventSettings(eventType: String, environment: Environment) {

    var processEnabled = setProperty(environment, eventType, PROCESS_ENABLED)?.toBoolean() ?: true
    var saveEnabled = setProperty(environment, eventType, SAVE_ENABLED)?.toBoolean() ?: true
    var loadEventsBatch = setProperty(environment, eventType, LOAD_EVENTS_BATCH)?.toInt() ?: 100
    var firstSleepDelay = setProperty(environment, eventType, FIRST_SLEEP_DELAY)?.toLong() ?: 0
    var repeatDelayOnLocked = setProperty(environment, eventType, REPEAT_DELAY_ON_LOCK)?.toLong() ?: 1000
    var repeatDelay = setProperty(environment, eventType, REPEAT_DELAY)?.toLong() ?: 0
    var repeatDelayOnEmpty = setProperty(environment, eventType, REPEAT_DELAY_ON_EMPTY)?.toLong() ?: 10000
    var nextDelay = setProperty(environment, eventType, NEXT_DELAY)?.toLong() ?: 10000
    var nextDelayCoefficient = setProperty(environment, eventType, NEXT_DELAY_COEFFICIENT)?.toFloat() ?: 1f
    var nextDelayIncrease = setProperty(environment, eventType, NEXT_DELAY_INCREASE)?.toBoolean() ?: true
    var poolCoreSize = setProperty(environment, eventType, POOL_CORE_SIZE)?.toInt() ?: 1
    var poolMaxSize = setProperty(environment, eventType, POOL_MAX_SIZE)?.toInt() ?: 2
    var coroutinesPerThread = setProperty(environment, eventType, COROUTINES_PER_THREAD)?.toInt() ?: 5
    var attemptsMax = setProperty(environment, eventType, ATTEMPTS_MAX)?.toLong() ?: 3
    var deleteAfterAttempts = setProperty(environment, eventType, DELETE_ATTEMPTS_MAX)?.toBoolean() ?: false
    var timeout = setProperty(environment, eventType, TIMEOUT)?.toLong() ?: 120000
    var executionType = ExecutionType.fromString(setProperty(environment, eventType, EXECUTION_TYPE))
    var stashName = setProperty(environment, eventType, STASH_NAME) ?: ("$eventType-HASH")

    private fun setProperty(environment: Environment, eventType: String, propertyName: String): String? {
        return environment.getProperty(OutboxEventProperties.getOutboxTypeProperty(propertyName, eventType))
            ?: environment.getProperty(propertyName)
    }

    fun isThreadPoolPropertyChanged(newSettings: OutboxEventSettings): Boolean {
        return !(newSettings.loadEventsBatch == this.loadEventsBatch
                && newSettings.poolCoreSize == this.poolCoreSize
                && newSettings.coroutinesPerThread == this.coroutinesPerThread
                && newSettings.poolMaxSize == this.poolMaxSize)
    }

    fun getNewNextTime(attempts: Long): Instant {
        if (attempts == 0L) {
            return Instant.now()
        }

        if (nextDelayIncrease) {
            val result = attempts * nextDelay * nextDelayCoefficient
            return Instant.now().plusMillis(result.toLong())
        }

        return Instant.now().plusMillis((nextDelay))
    }

    override fun toString(): String {
        return "OutboxEventSettings(processEnabled=$processEnabled, saveEnabled=$saveEnabled, " +
                "loadEventsBatch=$loadEventsBatch, firstSleepDelay=$firstSleepDelay," +
                " repeatDelayOnLocked=$repeatDelayOnLocked, repeatDelay=$repeatDelay, " +
                "repeatDelayOnEmpty=$repeatDelayOnEmpty, nextDelay=$nextDelay, nextDelayCoefficient=$nextDelayCoefficient, " +
                "nextDelayIncrease=$nextDelayIncrease, poolCoreSize=$poolCoreSize, poolMaxSize=$poolMaxSize, " +
                "coroutinesPerThread=$coroutinesPerThread, attemptsMax=$attemptsMax, deleteAfterAttempts=$deleteAfterAttempts," +
                " timeout=$timeout, executionType=$executionType, stashName='$stashName')"
    }


}