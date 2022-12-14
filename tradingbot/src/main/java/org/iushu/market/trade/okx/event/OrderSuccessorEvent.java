package org.iushu.market.trade.okx.event;

import org.springframework.context.ApplicationEvent;

public class OrderSuccessorEvent extends ApplicationEvent {

    private final String type;

    public OrderSuccessorEvent(String type, Object source) {
        super(source);
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
