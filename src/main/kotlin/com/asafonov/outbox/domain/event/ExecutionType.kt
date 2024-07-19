package com.asafonov.outbox.domain.event

/**
 * Method of processing events when multiple instances of the application are present.
 * @property EXCLUSIVE Only one instance processes the events.
 * @property PARALLEL All instances of the application process the queue simultaneously.
 */
enum class ExecutionType {

    EXCLUSIVE,
    PARALLEL;

    companion object {
        fun fromString(value: String?): ExecutionType {
            if (value.isNullOrEmpty()) {
                return PARALLEL
            }

            return ExecutionType.valueOf(value)
        }
    }
}