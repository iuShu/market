package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONObject;

import static org.iushu.trader.okx.OkxWsJsonClient.*;

public class OkxChannel extends JSONObject {

    public OkxChannel(String channel) {
        super(JSONObject.of(KEY_CHANNEL, channel));
    }

    public OkxChannel(String channel, String instId) {
        super(JSONObject.of(KEY_CHANNEL, channel, KEY_INST_ID, instId));
    }

    public OkxChannel(String channel, String instId, String instType) {
        super(JSONObject.of(KEY_CHANNEL, channel, KEY_INST_ID, instId, KEY_INST_TYPE, instType));
    }

    public String getChannel() {
        return getString(KEY_CHANNEL);
    }

    public String getInstId() {
        return getString(KEY_INST_ID);
    }

    public String getInstType() {
        return getString(KEY_INST_TYPE);
    }

    @Override
    public int hashCode() {
        return getChannel().concat(getInstId()).hashCode();
    }

}
