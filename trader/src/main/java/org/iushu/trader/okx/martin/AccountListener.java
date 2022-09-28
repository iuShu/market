package org.iushu.trader.okx.martin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.iushu.trader.okx.OkxWsJsonClient.KEY_DATA;

public class AccountListener implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AccountListener.class);

    @Override
    public JSONObject privateChannel() {
        return Setting.CHANNEL_ACCOUNT;
    }

    @Override
    public void consume(JSONObject message) {
        JSONArray data = message.getJSONArray(KEY_DATA);
        JSONObject stat = data.getJSONObject(0);
        JSONArray details = stat.getJSONArray("details");
        details.forEach(d -> {
            JSONObject detail = (JSONObject) d;
            if ("USDT".equals(detail.getString("ccy")))
                logger.info(detail.toJSONString());
        });
    }

}
