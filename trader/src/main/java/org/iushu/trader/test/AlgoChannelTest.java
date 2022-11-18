package org.iushu.trader.test;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlgoChannelTest implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AlgoChannelTest.class);

    @Override
    public JSONObject privateChannel() {
        return Setting.CHANNEL_ORDERS_ALGO;
    }

    @Override
    public void consume(JSONObject message) {
        logger.info(">> {}", message.toJSONString());
    }
}
