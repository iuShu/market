package org.iushu.market.client.event;

import org.springframework.context.ApplicationEvent;

public class ChannelEvent<T> extends ApplicationEvent {

    private final T payload;

    public ChannelEvent(Object source, T payload) {
        super(source);
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }

}
