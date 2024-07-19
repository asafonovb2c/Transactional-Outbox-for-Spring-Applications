package com.asafonov.outbox.application.event.impl;

import com.asafonov.outbox.application.OutboxEventHandleStrategy;
import com.asafonov.outbox.domain.event.OutboxEventResult;
import com.asafonov.outbox.domain.event.OutboxEventType;
import com.asafonov.outbox.domain.TestEventDtoSecond;
import com.asafonov.outbox.domain.TestOutboxEventType;
import org.jetbrains.annotations.NotNull;

import static com.asafonov.outbox.domain.event.OutboxEventResultKt.createEventProcessed;

public class TestOutboxEventHandleStrategySecond implements OutboxEventHandleStrategy<TestEventDtoSecond> {

    @NotNull
    @Override
    public OutboxEventResult handleEvent(TestEventDtoSecond eventDto) {
        System.out.println(eventDto.provideLockKey());
        return createEventProcessed();
    }

    @NotNull
    @Override
    public OutboxEventType getEventType() {
        return TestOutboxEventType.TEST_EVENT_SECOND;
    }

    @NotNull
    @Override
    public Class<TestEventDtoSecond> getEventClass() {
        return TestEventDtoSecond.class;
    }

}
