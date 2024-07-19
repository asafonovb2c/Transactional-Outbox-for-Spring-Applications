package com.asafonov.outbox.domain

import com.asafonov.outbox.domain.event.OutboxEventType

enum class TestOutboxEventType : OutboxEventType {
    TEST_EVENT {
        override fun getName(): String {
            return TEST_EVENT.name
        }
    },

    TEST_EVENT_SECOND {
        override fun getName(): String {
            return TEST_EVENT.name
        }
    }


}