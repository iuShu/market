package org.iushu.trader.websocket;

import com.alibaba.fastjson2.JSONObject;

public interface MessageConsumer {

    default void setClient(WsJsonClient client) {

    }

    JSONObject channel();

    void consume(JSONObject message);

}
