package org.iushu.market.client.event;

import java.time.Clock;

public class ChannelClosedEvent<T> extends ChannelEvent<T> {

    public ChannelClosedEvent(Object source, T payload, Clock clock) {
        super(source, payload, clock);
    }

}
