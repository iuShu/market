package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.iushu.market.Constants.ORDER_STATE_FILLED;
import static org.iushu.market.Constants.ORDER_STATE_LIVE;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_ORDERS;

@OkxComponent
public class Recorder {

    private static final Logger logger = LoggerFactory.getLogger(Recorder.class);

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void recordOrderActivity(JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        String state = data.getString("state");
        if (ORDER_STATE_FILLED.equals(state))
            return;

        String activity = ORDER_STATE_LIVE.equals(state) ? "placed" : "canceled";
        String px = data.getString("px");
        String side = data.getString("side");
        String ordId = data.getString("ordId");
        String posSide = data.getString("posSide");
        int position = data.getIntValue("sz", 0);
        logger.info("{} order {} {} {} {} {}", activity, posSide, side, position, px, ordId);
    }

}
