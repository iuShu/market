package org.iushu.trader.websocket;

import com.alibaba.fastjson2.JSONObject;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class PongDecoder implements Decoder.Text<JSONObject> {

    private static final JSONObject EMPTY = JSONObject.of();

    @Override
    public JSONObject decode(String s) {
        return EMPTY;
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    @Override
    public void init(EndpointConfig endpointConfig) {

    }

    @Override
    public void destroy() {

    }
}
