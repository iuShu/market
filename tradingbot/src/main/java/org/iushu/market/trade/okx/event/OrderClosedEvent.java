package org.iushu.market.trade.okx.event;

import org.springframework.context.ApplicationEvent;

public class OrderClosedEvent extends ApplicationEvent {

    public OrderClosedEvent(Object source) {
        super(source);
    }

}
