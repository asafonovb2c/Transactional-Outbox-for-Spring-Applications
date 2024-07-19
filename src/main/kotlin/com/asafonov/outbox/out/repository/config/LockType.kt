package com.asafonov.outbox.out.repository.config

/**
 * @property REDIS Redis, distributed lock.
 * @property LOCAL Local lock within the application, suitable only if there is a single instance.
 */
enum class LockType {
    REDIS,
    LOCAL
}