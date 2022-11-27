package org.iushu.market.trade.okx.event;

import org.springframework.context.ApplicationEvent;

public class OrderErrorEvent extends ApplicationEvent {

    public OrderErrorEvent(Object source) {
        super(source);
    }

}
