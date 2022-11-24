package org.iushu.market.client.event;

import java.time.Clock;

public class ChannelOpenedEvent<T> extends ChannelEvent<T> {

    public ChannelOpenedEvent(Object source, T payload, Clock clock) {
        super(source, payload, clock);
    }

}
