package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OrderFilledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;

import static org.iushu.market.Constants.ORDER_STATE_FILLED;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_ORDERS;

@OkxComponent
public class Tracker implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Tracker.class);

    private final TradingProperties properties;
    private ApplicationEventPublisher eventPublisher;
    private volatile double firstPx = 0.0;

    public Tracker(TradingProperties properties) {
        this.properties = properties;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onOrderFilled(OkxWebSocketSession session, JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        int pos = data.getIntValue("sz", -1);
        String ordId = data.getString("ordId");
        String state = data.getString("state");
        String side = data.getString("side");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        if (pos != properties.getOrder().getPosStart() || !ORDER_STATE_FILLED.equals(state) || !side.equals(posSide.openSide()))
            return;

        placeFollowOrders();
        placeAlgoOrder();
        addMarginBalance();
        eventPublisher.publishEvent(new OrderFilledEvent(data));
    }

    private void placeFollowOrders() {

    }

    private void addMarginBalance() {

    }

    private void placeAlgoOrder() {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
