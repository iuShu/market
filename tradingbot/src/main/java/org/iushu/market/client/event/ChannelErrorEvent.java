package org.iushu.market.client.event;

import java.time.Clock;

public class ChannelErrorEvent<T> extends ChannelEvent<T> {

    public ChannelErrorEvent(Object source, T payload, Clock clock) {
        super(source, payload, clock);
    }

}
