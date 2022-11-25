package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxConstants;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.PacketUtils;
import org.iushu.market.trade.okx.Strategy;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;

import static org.iushu.market.trade.okx.OkxConstants.*;

@OkxComponent
public class Initiator {

    private final Strategy<Double> strategy;
    private volatile String messageId = "";
    private volatile boolean existed = false;

    public Initiator(Strategy<Double> strategy) {
        this.strategy = strategy;
    }

    @SubscribeChannel(channel = CHANNEL_TICKERS)
    public void placeFirstOrder(OkxWebSocketSession session, JSONObject message) {
        if (existed)
            return;

        JSONArray data = message.getJSONArray("data");
        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");
        PosSide posSide = strategy.trend(price);
        if (posSide == null)
            return;

        // send order-placing packet
    }

    @SubscribeChannel(op = OP_ORDER)
    public void placeResponse(JSONObject message) {
        String mid = message.getString("id");
        if (!messageId.equals(mid))
            return;

        int code = message.getIntValue("code", -1);
        if (SUCCESS != code)
            return;

        existed = true;
        messageId = "";
    }

}
