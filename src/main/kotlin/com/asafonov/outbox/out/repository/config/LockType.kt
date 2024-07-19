package com.asafonov.outbox.out.repository.config

/**
 * @property REDIS Редис, распределнный лок
 * @property LOCAL Лок внутри приложения, подходит только если инстанс = 1
 */
enum class LockType {
    REDIS,
    LOCAL
}