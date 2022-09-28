package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.websocket.MessageConsumer;
import org.iushu.trader.websocket.WsJsonClient;

public interface OkxMessageConsumer extends MessageConsumer {

    default void setClient(WsJsonClient client) {
        setPublicClient(client);
    }

    default void setPublicClient(WsJsonClient client) {

    }

    default void setPrivateClient(WsJsonClient client) {

    }

    default JSONObject channel() {
        return publicChannel();
    }

    default JSONObject publicChannel() {
        return null;
    }

    default JSONObject privateChannel() {
        return null;
    }

}
