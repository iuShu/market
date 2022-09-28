package org.iushu.trader.okx.martin;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.websocket.WsJsonClient;

public class AlgoProcessor implements OkxMessageConsumer {

    private WsJsonClient client;

    @Override
    public JSONObject privateChannel() {
        return Setting.CHANNEL_ORDERS_ALGO;
    }

    @Override
    public void setPrivateClient(WsJsonClient client) {
        this.client = client;
    }

    @Override
    public void consume(JSONObject message) {

    }
}
