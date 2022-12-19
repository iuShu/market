package org.iushu.market.client.event;

public class ChannelReconnectEvent<T> extends ChannelEvent<T> {

    public ChannelReconnectEvent(Object source, T payload) {
        super(source, payload);
    }

}
