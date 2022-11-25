package org.iushu.market.trade.okx.event;

import org.springframework.context.ApplicationEvent;

import java.time.Clock;

public class OrderClosedEvent extends ApplicationEvent {

    public OrderClosedEvent(Object source, Clock clock) {
        super(source, clock);
    }

}
