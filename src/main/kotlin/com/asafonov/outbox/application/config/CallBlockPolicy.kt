package com.asafonov.outbox.application.config


import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CallBlockPolicy(private var maxWait: Long = 60000) : RejectedExecutionHandler {

    override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
        if (!executor.isShutdown) {
            try {
                val queue = executor.queue
                if (!queue.offer(r, this.maxWait, TimeUnit.MILLISECONDS)) {
                    throw RejectedExecutionException("Max wait time expired to queue task")
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw RejectedExecutionException("Interrupted", e)
            }
        } else {
            throw RejectedExecutionException("Executor has been shut down")
        }
    }
}