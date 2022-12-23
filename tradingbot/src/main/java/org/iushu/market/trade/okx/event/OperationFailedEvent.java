package org.iushu.market.trade.okx.event;

import org.springframework.context.ApplicationEvent;

public class OperationFailedEvent extends ApplicationEvent {

    public OperationFailedEvent(Object source) {
        super(source);
    }

}
