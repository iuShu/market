package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.iushu.market.Constants.*;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_ORDERS;

@OkxComponent
public class Tracker {

    private static final Logger logger = LoggerFactory.getLogger(Tracker.class);

    private TradingProperties properties;

    public Tracker(TradingProperties properties) {
        this.properties = properties;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onOrderMessage(OkxWebSocketSession session, JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        String state = data.getString("state");
        if (ORDER_STATE_FILLED.equals(state))
            orderFilled(session, data);
    }

    private void orderFilled(OkxWebSocketSession session, JSONObject data) {

    }

    private void placeFollowOrders() {

    }

    private void addMarginBalance() {

    }

    private void placeAlgoOrder() {

    }

}
