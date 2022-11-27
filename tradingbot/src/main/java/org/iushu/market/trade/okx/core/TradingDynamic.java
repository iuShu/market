package org.iushu.market.trade.okx.core;

import org.iushu.market.component.notify.Notifier;
import org.iushu.market.trade.okx.DispatchManager;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderErrorEvent;
import org.iushu.market.trade.okx.event.OrderFilledEvent;
import org.springframework.context.event.EventListener;

@OkxComponent
public class TradingDynamic {

    private Notifier notifier;
    private DispatchManager manager;

    public TradingDynamic(Notifier notifier, DispatchManager manager) {
        this.notifier = notifier;
        this.manager = manager;
    }

    @EventListener(OrderFilledEvent.class)
    public void notifyOrderFilled(OrderFilledEvent event) {

    }

    @EventListener(OrderClosedEvent.class)
    public void notifyOrderClosed(OrderClosedEvent event) {

    }

    @EventListener(OrderErrorEvent.class)
    public void notifyOrderError(OrderErrorEvent event) {
        Object source = event.getSource();
        if (source instanceof String)
            notifier.notify("Order Error", source.toString());
        // TODO message template
        manager.close();
    }

}
