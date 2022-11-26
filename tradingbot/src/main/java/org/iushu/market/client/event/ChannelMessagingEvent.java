package org.iushu.market.client.event;

public class ChannelMessagingEvent<T> extends ChannelEvent<T> {

    public ChannelMessagingEvent(Object source, T payload) {
        super(source, payload);
    }

}
