package org.iushu.market.client;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.client.event.ChannelClosedEvent;
import org.iushu.market.client.event.ChannelErrorEvent;
import org.iushu.market.client.event.ChannelMessagingEvent;
import org.iushu.market.client.event.ChannelOpenedEvent;
import org.iushu.market.config.WebSocketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;

public class ChannelWebSocketHandler implements WebSocketHandler, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(ChannelWebSocketHandler.class);

    protected WebSocketProperties properties;
    protected WebSocketClient client;
    protected WebSocketSession session;
    protected String websocketUrl;
    protected ApplicationEventPublisher eventPublisher;

    protected int reconnectTimes = 0;

    public ChannelWebSocketHandler(WebSocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("connect established {}", session.getId());
        this.session = session;
        this.eventPublisher.publishEvent(ChannelOpenedEvent.of(this, session));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        logger.debug("{}", message.toString());
        if (message instanceof TextMessage) {
            JSONObject payload = JSONObject.parseObject(message.getPayload().toString());
            this.eventPublisher.publishEvent(ChannelMessagingEvent.of(session, payload));
        }
        else if (message instanceof PongMessage) {
            // ignore
        }
        else {
            logger.warn("recv message of unknown type {}", message.getClass().getName());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.warn("transport error", exception);
        this.eventPublisher.publishEvent(ChannelErrorEvent.of(session, exception));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.warn("connection closed due to {} {}", closeStatus.getCode(), closeStatus.getReason());
        if (reconnectTimes >= properties.getReconnectTime()) {
            logger.warn("client reached max reconnect times, over");
            this.eventPublisher.publishEvent(ChannelClosedEvent.of(session, closeStatus));
            return;
        }

        logger.info("client disconnected, {} try reconnecting", reconnectTimes);
        client.doHandshake(this, websocketUrl);
        reconnectTimes++;
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    protected boolean isActive() {
        return session != null && session.isOpen();
    }

    public void setClient(WebSocketClient client) {
        this.client = client;
    }

    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }

    public String getWebsocketUrl() {
        return websocketUrl;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }

}
