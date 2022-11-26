package org.iushu.market.client.event;

public class ChannelClosedEvent<T> extends ChannelEvent<T> {

    public ChannelClosedEvent(Object source, T payload) {
        super(source, payload);
    }

}
