package org.iushu.trader.test;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.OkxHttpUtils;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class OrdersChannelTest implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrdersChannelTest.class);

    private WsJsonClient client;

    public OrdersChannelTest() {
    }

    @Override
    public void setPrivateClient(WsJsonClient client) {
        this.client = client;
    }

    @Override
    public JSONObject privateChannel() {
        return Setting.CHANNEL_ORDERS;
    }

    @Override
    public void consume(JSONObject message) {
        logger.info(">> {}", message.toJSONString());

        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        String state = data.getString("state");
        String side = data.getString("side");
        int position = data.getIntValue("sz");
        double fillSize = data.getDoubleValue("fillSz");
        if (fillSize < position || !"filled".equals(state) || !Constants.SIDE_SELL.equals(side))
            return;

        double fillPx = data.getDoubleValue("fillPx");
        JSONObject resp = OkxHttpUtils.placeAlgoOrder(PosSide.ShortSide, Constants.SIDE_BUY, position,
                fillPx - 50, fillPx + 50);  // for test only
        if (!resp.isEmpty())
            logger.info("place algo order {} for {}", resp.getString("algoId"), position);

        try {
            TimeUnit.SECONDS.sleep(12);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean canceled = OkxHttpUtils.cancelAlgoOrder(resp.getString("algoId"));
        logger.info("canceled algo {}", canceled);
        System.exit(1);
    }
}
