package org.iushu.market.client.event;

public class ChannelOpenedEvent<T> extends ChannelEvent<T> {

    public ChannelOpenedEvent(Object source, T payload) {
        super(source, payload);
    }

}
