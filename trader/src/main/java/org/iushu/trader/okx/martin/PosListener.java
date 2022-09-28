package org.iushu.trader.okx.martin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.iushu.trader.okx.OkxWsJsonClient.KEY_DATA;

public class PosListener implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PosListener.class);

    @Override
    public JSONObject privateChannel() {
        return Setting.CHANNEL_POSITIONS;
    }

    @Override
    public void consume(JSONObject message) {
        logger.info(message.toJSONString());
        JSONArray data = message.getJSONArray(KEY_DATA);
        if (data.isEmpty())
            return;

        // TODO check whether the orders has been closed
    }

}
