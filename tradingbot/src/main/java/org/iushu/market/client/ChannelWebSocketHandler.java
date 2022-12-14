package org.iushu.market.client;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.client.event.ChannelErrorEvent;
import org.iushu.market.client.event.ChannelMessagingEvent;
import org.iushu.market.client.event.ChannelOpenedEvent;
import org.iushu.market.client.event.ChannelReconnectEvent;
import org.iushu.market.config.WebSocketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;

import java.util.Date;

public class ChannelWebSocketHandler implements WebSocketHandler, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(ChannelWebSocketHandler.class);

    public static final CloseStatus INITIATE_CLOSE = new CloseStatus(4444);

    protected WebSocketProperties properties;
    protected TaskScheduler taskScheduler;
    protected WebSocketClient client;
    protected WebSocketSession session;
    protected String websocketUrl;
    protected ApplicationEventPublisher eventPublisher;

    protected int reconnectTimes = 0;

    public ChannelWebSocketHandler(WebSocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("connect established {}", websocketUrl);
        if (reconnectTimes != 0)
            eventPublisher.publishEvent(new ChannelReconnectEvent<>(session, reconnectTimes));
        reconnectTimes = 0;
        this.session = session;
        this.eventPublisher.publishEvent(new ChannelOpenedEvent<>(this, session));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        logger.debug("<< {}", message.getPayload().toString());
        if (message instanceof TextMessage) {
            JSONObject payload = JSONObject.parseObject(message.getPayload().toString());
            try {
                this.eventPublisher.publishEvent(new ChannelMessagingEvent<>(session, payload));
            } catch (TaskRejectedException e) {
                logger.warn("messaging task rejected");
            }
        }
        else if (message instanceof PongMessage) {
            // ignore
        }
        else {
            logger.warn("recv message of unknown type {}", message.getClass().getName());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.warn("transport error", exception);
//        this.eventPublisher.publishEvent(new ChannelErrorEvent<>(session, exception));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        logger.warn("connection closed due to {} {}", closeStatus.getCode(), closeStatus.getReason());
        if (!INITIATE_CLOSE.equalsCode(closeStatus))
            reconnect();
    }

    public void reconnect() {
        logger.info("client disconnected to {}", websocketUrl);
        long next = reconnectTimes != 0 && reconnectTimes % properties.getReconnectTime() == 0 ? 10000 : 2000;
        taskScheduler.schedule(() -> {
            logger.info("try reconnect to {} [{}]", websocketUrl, reconnectTimes);
            client.doHandshake(this, websocketUrl);
            reconnectTimes++;
        }, new Date(System.currentTimeMillis() + next));
        if (reconnectTimes != 0 && reconnectTimes % 20 == 0)
            eventPublisher.publishEvent(new ChannelErrorEvent<>(this, reconnectTimes));
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void setClient(WebSocketClient client) {
        this.client = client;
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
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
