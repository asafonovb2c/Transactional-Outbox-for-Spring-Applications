package com.asafonov.outbox.domain

import com.asafonov.outbox.domain.event.OutboxEventDto

class TestEventDto(var uuid : String) : OutboxEventDto {

    override fun provideLockKey(): String {
        return uuid
    }
}