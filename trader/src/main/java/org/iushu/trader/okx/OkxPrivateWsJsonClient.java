package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.websocket.MessageConsumer;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class OkxPrivateWsJsonClient extends WsJsonClient {

    private static final Logger logger = LoggerFactory.getLogger(OkxPrivateWsJsonClient.class);

    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_INST_ID = "instId";
    public static final String KEY_INST_TYPE = "instType";
    public static final String KEY_OP = "op";
    public static final String KEY_ARGS = "args";
    public static final String KEY_EVENT = "event";
    public static final String KEY_ARG = "arg";
    public static final String KEY_DATA = "data";

    private Consumer<WsJsonClient> afterLoginTask;

    public void afterLogin(Consumer<WsJsonClient> task) {
        this.afterLoginTask = task;
    }

    @Override
    public String wsUrl() {
        return Setting.WSS_PRIVATE_URL;
    }

    @Override
    protected Object openKey(JSONObject data) {
        return ((OkxChannel) data).getChannel().hashCode();
    }

    @Override
    public Object recvKey(JSONObject data) {
        if (!data.containsKey(KEY_ARG))
            return -1;
        JSONObject arg = data.getJSONObject(KEY_ARG);
        return arg.getString(KEY_CHANNEL).hashCode();
    }

    @Override
    public void onOpen(EndpointConfig config) {
        send(PacketUtils.loginPacket());
        heartbeat(5, TimeUnit.SECONDS);
    }

    @Override
    public boolean onMessage(JSONObject message) {
//        logger.info("onRecv: {}", message.toJSONString());
        String event = message.getString("event");
        if (event == null || event.isEmpty())
            return true;

        switch (event) {
            case "login":
                logger.info("login ok");
                JSONArray channels = new JSONArray();
                this.consumers.values().forEach(list -> {
                    OkxMessageConsumer messageConsumer = (OkxMessageConsumer) list.get(0);
                    OkxChannel channel = (OkxChannel) messageConsumer.privateChannel();
                    channels.add(channel);
                });
                if (!send(JSONObject.of(KEY_OP, "subscribe", KEY_ARGS, channels))) {
                    logger.error("send subscribe message failed");
                    shutdown();
                }
                this.applyTask();
                break;
            case "subscribe":
                JSONObject arg = message.getJSONObject("arg");
                logger.info("subscribed {} {}", arg.getString("channel"), arg.getString("instId"));
                break;
            case "error":
                logger.error("login error, {}", message.toJSONString());
                shutdown();
                break;
        }
        return false;
    }

    @Override
    public void register(MessageConsumer consumer) {
        OkxMessageConsumer okxMessageConsumer = (OkxMessageConsumer) consumer;
        JSONObject channel = okxMessageConsumer.privateChannel();
        if (channel != null) {
            Object key = Objects.requireNonNull(openKey(channel));
            List<MessageConsumer> registers = this.consumers.computeIfAbsent(key, list -> new CopyOnWriteArrayList<>());
            registers.add(okxMessageConsumer);
        }
        okxMessageConsumer.setPrivateClient(this);
    }

    @Override
    public void unregister(MessageConsumer consumer) {
        OkxMessageConsumer okxMessageConsumer = (OkxMessageConsumer) consumer;
        JSONObject channel = okxMessageConsumer.privateChannel();
        List<MessageConsumer> registers = this.consumers.get(Objects.requireNonNull(openKey(channel)));
        if (registers != null && !registers.isEmpty())
            registers.remove(okxMessageConsumer);
        okxMessageConsumer.setPrivateClient(null);
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

    private void applyTask() {
        if (this.afterLoginTask != null) {
            this.afterLoginTask.accept(this);
            this.afterLoginTask = null;
        }
    }

}
