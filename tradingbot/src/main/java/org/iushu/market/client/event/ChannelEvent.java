package org.iushu.market.client.event;

import org.springframework.context.ApplicationEvent;

import java.time.Clock;

public class ChannelEvent<T> extends ApplicationEvent {

    private T payload;

    public ChannelEvent(Object source, T payload) {
        super(source);
        this.payload = payload;
    }

    public ChannelEvent(Object source, T payload, Clock clock) {
        super(source, clock);
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public static <T> ChannelEvent<T> of(Object source, T payload) {
        return new ChannelEvent<>(source, payload);
    }

}
