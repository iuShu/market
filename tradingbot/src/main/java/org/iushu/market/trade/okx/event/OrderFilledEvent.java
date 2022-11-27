package org.iushu.market.trade.okx.event;

import org.springframework.context.ApplicationEvent;

public class OrderFilledEvent extends ApplicationEvent {

    public OrderFilledEvent(Object source) {
        super(source);
    }

}
