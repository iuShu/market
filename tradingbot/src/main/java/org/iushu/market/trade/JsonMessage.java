package org.iushu.market.trade;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.socket.WebSocketMessage;

public class JsonMessage implements WebSocketMessage<JSONObject> {

    private JSONObject jsonObject;

    private JsonMessage(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public JSONObject getPayload() {
        return this.jsonObject;
    }

    @Override
    public int getPayloadLength() {
        return this.jsonObject.toString().length();
    }

    @Override
    public boolean isLast() {
        return false;
    }

    public static JsonMessage of(JSONObject jsonObject) {
        return new JsonMessage(jsonObject);
    }

}
