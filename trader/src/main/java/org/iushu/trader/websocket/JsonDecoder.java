package org.iushu.trader.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import javax.websocket.Decoder.Text;
import javax.websocket.EndpointConfig;

public class JsonDecoder implements Text<JSONObject> {

    @Override
    public JSONObject decode(String s) {
        return JSON.parseObject(s);
    }

    @Override
    public boolean willDecode(String s) {
        return s.startsWith("{") && s.endsWith("}");
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
