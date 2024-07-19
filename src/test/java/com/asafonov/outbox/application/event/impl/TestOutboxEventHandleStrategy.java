package com.asafonov.outbox.application.event.impl;

import com.asafonov.outbox.application.OutboxEventHandleStrategy;
import com.asafonov.outbox.domain.event.OutboxEventResult;
import com.asafonov.outbox.domain.event.OutboxEventType;
import com.asafonov.outbox.domain.TestEventDto;
import com.asafonov.outbox.domain.TestOutboxEventType;
import org.jetbrains.annotations.NotNull;

import static com.asafonov.outbox.domain.event.OutboxEventResultKt.createEventProcessed;

public class TestOutboxEventHandleStrategy implements OutboxEventHandleStrategy<TestEventDto> {

    @NotNull
    @Override
    public OutboxEventResult handleEvent(TestEventDto eventDto) {
        System.out.println(eventDto.provideLockKey());
        return createEventProcessed();
    }

    @NotNull
    @Override
    public OutboxEventType getEventType() {
        return TestOutboxEventType.TEST_EVENT;
    }

    @NotNull
    @Override
    public Class<TestEventDto> getEventClass() {
        return TestEventDto.class;
    }

}