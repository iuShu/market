package org.iushu.market.trade.okx.event;

import org.springframework.context.ApplicationEvent;

public class TradingStopEvent extends ApplicationEvent {

    private static final TradingStopEvent INSTANCE = new TradingStopEvent();

    private TradingStopEvent() {
        super("trading stop");
    }

    public static TradingStopEvent event() {
        return INSTANCE;
    }

}
