package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;

import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_ORDERS;

@OkxComponent
public class Tracker {

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onOrderPlaced(OkxWebSocketSession session, JSONObject message) {

    }

}
