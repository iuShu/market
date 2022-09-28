package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.SyncControl;
import org.iushu.trader.SyncController;
import org.iushu.trader.websocket.MessageConsumer;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;

public class OkxWsJsonClient extends WsJsonClient implements SyncControl {

    private static final Logger logger = LoggerFactory.getLogger(OkxWsJsonClient.class);

    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_INST_ID = "instId";
    public static final String KEY_INST_TYPE = "instType";
    public static final String KEY_OP = "op";
    public static final String KEY_ARGS = "args";
    public static final String KEY_EVENT = "event";
    public static final String KEY_ARG = "arg";
    public static final String KEY_DATA = "data";

    public OkxWsJsonClient() {
        SyncController.instance().register(this);
    }

    @Override
    public String wsUrl() {
        return Setting.WSS_PUBLIC_URL;
    }

    @Override
    protected Object openKey(JSONObject data) {
        return ((OkxChannel) data).getChannel().hashCode();
    }

    @Override
    public Object recvKey(JSONObject data) {
        JSONObject arg = data.getJSONObject(KEY_ARG);
        return arg.getString(KEY_CHANNEL).hashCode();
    }

    @Override
    public void onOpen(EndpointConfig config) {
        JSONArray channels = new JSONArray();
        this.consumers.values().forEach(list -> {
            MessageConsumer messageConsumer = list.get(0);
            OkxChannel channel = (OkxChannel) messageConsumer.channel();
            channels.add(channel);
        });
        if (!send(JSONObject.of(KEY_OP, "subscribe", KEY_ARGS, channels))) {
            logger.error("send subscribe message failed");
            shutdown();
        }
    }

    @Override
    public boolean onMessage(JSONObject message) {
//        logger.info("onRecv: {}", message.toJSONString());
        String event = message.getString("event");
        if (event == null || event.isEmpty())
            return true;

        switch (event) {
            case "subscribe":
                JSONObject arg = message.getJSONObject("arg");
                logger.info("subscribed {} {}", arg.getString("channel"), arg.getString("instId"));
                break;
            case "error":
                logger.error("subscribe error, {}", message.toJSONString());
                break;
        }
        return false;
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("onError", throwable);
        throwable.printStackTrace();
    }

    @Override
    public void onClose(CloseReason closeReason) {
        logger.error("onClose {}", closeReason);
    }

    @Override
    public void shutdown() {
        SyncController.instance().shutdown();
    }

    @Override
    public void syncShutdown() {
        super.shutdown();
    }

}
