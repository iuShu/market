package org.iushu.market.client.event;

import java.time.Clock;

public class ChannelMessagingEvent<T> extends ChannelEvent<T> {

    public ChannelMessagingEvent(Object source, T payload, Clock clock) {
        super(source, payload, clock);
    }

}
