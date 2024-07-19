package com.asafonov.outbox.domain

import com.asafonov.outbox.domain.event.OutboxEventDto
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class TestEventDtoSecond(@JsonProperty("uuid") var uuid : String) : OutboxEventDto {

    @JsonIgnore
    override fun provideLockKey(): String {
        return uuid
    }
}