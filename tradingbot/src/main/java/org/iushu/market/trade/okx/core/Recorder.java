package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.iushu.market.Constants.*;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_ORDERS;

@OkxComponent
public class Recorder {

    private static final Logger logger = LoggerFactory.getLogger(Recorder.class);

    private static final Map<String, String> STATE_DESC = new HashMap<>();

    static {
        STATE_DESC.put(ORDER_STATE_LIVE, "placed");
        STATE_DESC.put(ORDER_STATE_FILLED, "filled");
        STATE_DESC.put(ORDER_STATE_CANCELED, "canceled");
        STATE_DESC.put(ORDER_STATE_PARTIALLY_FILLED, "partial filled");
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void recordOrderActivity(JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        String state = data.getString("state");
        String activity = STATE_DESC.get(state);
        if (activity == null) {
            logger.warn("unknown state data {}", data);
            activity = state;
        }
        String px = ORDER_STATE_FILLED.equals(state) ? data.getString("avgPx") : data.getString("px");
        String side = data.getString("side");
        String ordId = data.getString("ordId");
        String posSide = data.getString("posSide");
        int contractSize = data.getIntValue("sz", 0);
        logger.info("{} order {} {} {} {} {}", activity, posSide, side, contractSize, px, ordId);
    }

}
