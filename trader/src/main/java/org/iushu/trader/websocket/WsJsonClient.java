package org.iushu.trader.websocket;

import com.alibaba.fastjson2.JSONObject;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.InstanceManagerBindings;
import org.apache.tomcat.websocket.pojo.PojoEndpointClient;
import org.iushu.trader.base.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import static java.util.Collections.singletonList;

public abstract class WsJsonClient {

    private static final Logger logger = LoggerFactory.getLogger(WsJsonClient.class);
    private static final int RECONNECT_INTERVAL = 2;
    private static final int MAX_RETRY_TIMES = 5;

    private InstanceManager instanceManager;
    private ClientEndpointConfig endpointConfig;
    private Endpoint endpoint;
    private Session session;

    private final ExecutorService executor = DefaultExecutor.executor();
    private boolean reconnect = true;
    private int reconnect_times = 0;
    protected Map<Object, List<MessageConsumer>> consumers = new ConcurrentHashMap<>();

    public WsJsonClient() {
        try {
            List<Class<? extends Decoder>> decoders = new ArrayList<>();
            decoders.add(JsonDecoder.class);
            decoders.add(PongDecoder.class);
            this.endpointConfig = ClientEndpointConfig.Builder.create()
                    .decoders(decoders)
                    .encoders(singletonList(JsonEncoder.class)).build();
            this.endpoint = new PojoEndpointClient(this, endpointConfig.getDecoders(), instanceManager());
        } catch (DeploymentException e) {
            e.printStackTrace();
        }
    }

    public Session getSession() {
        return this.session;
    }

    protected void heartbeat(long interval, TimeUnit timeUnit) {
        DefaultExecutor.scheduler().scheduleAtFixedRate(() -> {
            if (session != null && session.isOpen())
                session.getAsyncRemote().sendText("ping");
        }, 0, interval, timeUnit);
    }

    @OnOpen
    public void _onOpen(Session session, EndpointConfig config) {
        this.session = session;
        this.onOpen(config);
    }

    @OnMessage
    public void _onReceive(Session session, JSONObject message) {
//        logger.debug(">> {}", message.toJSONString());
        if (message.isEmpty())
            return;

        if (!this.onMessage(message))
            return;

        Object key = Objects.requireNonNull(recvKey(message));
        List<MessageConsumer> consumers;
        if (key instanceof Integer && ((int) key) == -1) {
            consumers = new ArrayList<>();
            this.consumers.values().forEach(consumers::addAll);
        }
        else {
            consumers = this.consumers.get(key);
        }

        if (consumers == null || consumers.isEmpty())
            return;

        consumers.forEach(c -> executor.submit(() -> {
            try {
                c.consume(message);
            } catch (Exception e) {
                logger.error("{} consume error at {}", c.getClass().getName(), message, e);
                this.shutdown();
            }
        }));
    }

    @OnError
    public void _onError(Session session, Throwable throwable) {
        this.onError(throwable);
        if (session != null && !session.isOpen())
            this.tryReconnect();
    }

    @OnClose
    public void _onClose(Session session, CloseReason closeReason) {
        this.onClose(closeReason);
        this.tryReconnect();
    }

    public void onOpen(EndpointConfig config) {

    }

    public boolean onMessage(JSONObject message) {
        return true;
    }

    public void onError(Throwable throwable) {

    }

    public void onClose(CloseReason closeReason) {

    }

    public boolean send(JSONObject message) {
        if (session == null || !session.isOpen())
            return false;
        try {
            session.getBasicRemote().sendObject(message);
            return true;
        } catch (IOException | EncodeException e) {
            logger.error("send message failed", e);
        }
        return false;
    }

    public void sendAsync(JSONObject message, SendHandler sendHandler) {
        if (session == null || !session.isOpen()) {
            sendHandler.onResult(new SendResult(new ClosedChannelException()));
            return;
        }
        try {
            session.getAsyncRemote().sendObject(message, sendHandler);
        } catch (Exception e) {
            logger.error("send message failed", e);
        }
    }

    public void register(MessageConsumer consumer) {
        JSONObject channel = consumer.channel();
        Object key = Objects.requireNonNull(openKey(channel));
        List<MessageConsumer> registers = this.consumers.computeIfAbsent(key, list -> new CopyOnWriteArrayList<>());
        registers.add(consumer);
        consumer.setClient(this);
    }

    public void unregister(MessageConsumer consumer) {
        JSONObject channel = consumer.channel();
        List<MessageConsumer> registers = this.consumers.get(Objects.requireNonNull(openKey(channel)));
        if (registers != null && !registers.isEmpty())
            registers.remove(consumer);
        consumer.setClient(null);
    }

    public void start() {
        String ws_url = wsUrl();
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(endpoint, endpointConfig, URI.create(ws_url));
            logger.info("ws client connect finished");
            this.reconnect_times = 0;
        } catch (Exception e) {
            if (e instanceof DeploymentException && e.getCause() instanceof TimeoutException)
                logger.error("connect server timeout");
            else
                logger.error("connect to server failed", e);
            this.tryReconnect();
        }
    }

    public void shutdown() {
        try {
            logger.warn("{} shutdown ws client manually", this.getClass().getName());
            this.reconnect = false;
            if (this.session != null && this.session.isOpen())
                this.session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void tryReconnect() {
        if (!this.reconnect)
            return;
        if (this.reconnect_times >= MAX_RETRY_TIMES) {
            this.shutdown();
            return;
        }

        this.reconnect_times += 1;
        logger.warn("{}s later try reconnect to {}", RECONNECT_INTERVAL, wsUrl());
        DefaultExecutor.scheduler().schedule(this::start, RECONNECT_INTERVAL, TimeUnit.SECONDS);
    }

    private InstanceManager instanceManager() {
        if (instanceManager == null) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            instanceManager = InstanceManagerBindings.get(contextClassLoader);
        }
        return instanceManager;
    }

    public abstract String wsUrl();

    protected abstract Object openKey(JSONObject data);

    protected abstract Object recvKey(JSONObject message);

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }
}
