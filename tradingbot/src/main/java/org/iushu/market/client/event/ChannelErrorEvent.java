package org.iushu.market.client.event;

public class ChannelErrorEvent<T> extends ChannelEvent<T> {

    public ChannelErrorEvent(Object source, T payload) {
        super(source, payload);
    }

}
