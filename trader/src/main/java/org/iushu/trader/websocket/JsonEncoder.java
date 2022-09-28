package org.iushu.trader.websocket;

import com.alibaba.fastjson2.JSONObject;
import javax.websocket.Encoder.Text;
import javax.websocket.EndpointConfig;

public class JsonEncoder implements Text<JSONObject> {

    @Override
    public String encode(JSONObject object) {
        return object.toString();
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
