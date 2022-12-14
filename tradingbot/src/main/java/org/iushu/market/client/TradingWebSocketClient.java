package org.iushu.market.client;

import org.iushu.market.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TradingWebSocketClient extends StandardWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(TradingWebSocketClient.class);

    private String wsUrl;
    private TaskScheduler taskScheduler;
    private TradingProperties properties;
    private WebSocketSession session;
    private WebSocketHandler webSocketHandler;
    private ScheduledFuture<?> heartbeatFuture;
    private TradingWebSocketClient.HeartbeatTask heartbeatTask;

    public ListenableFuture<WebSocketSession> doHandshake() {
        return doHandshake(this.webSocketHandler, wsUrl);
    }

    @Override
    public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVars) {
        ChannelWebSocketHandler channelWebSocketHandler = (ChannelWebSocketHandler) webSocketHandler;
        channelWebSocketHandler.setClient(this);
        channelWebSocketHandler.setTaskScheduler(taskScheduler);
        channelWebSocketHandler.setWebsocketUrl(uriTemplate);
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("x-simulated-trading", "1");
        ListenableFuture<WebSocketSession> doHandshake = super.doHandshake(webSocketHandler, headers, URI.create(uriTemplate));
        try {
            WebSocketSession raw = doHandshake.get(30, TimeUnit.SECONDS);
            this.session = new WrapWebSocketSession(raw);
            logger.info("heartbeat scheduled {}", wsUrl);
            scheduleHeartbeat();
        } catch (Exception e) {
            logger.error("{} connect failed", wsUrl);
            channelWebSocketHandler.reconnect();
        }
        return doHandshake;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public void setProperties(TradingProperties properties) {
        this.properties = properties;
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void setWebSocketHandler(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public WebSocketHandler getWebSocketHandler() {
        return webSocketHandler;
    }

    private void scheduleHeartbeat() {
        cancelHeartbeat();
        if (!isActive())
            return;

        Date time = new Date(System.currentTimeMillis() + properties.getHeartbeatPeriod());
        this.heartbeatTask = new TradingWebSocketClient.HeartbeatTask();
        this.heartbeatFuture = this.taskScheduler.schedule(this.heartbeatTask, time);
    }

    private void cancelHeartbeat() {
        if (this.heartbeatFuture != null) {
            this.heartbeatFuture.cancel(false);
            this.heartbeatFuture = null;
        }
        if (this.heartbeatTask != null) {
            this.heartbeatTask.cancel();
            this.heartbeatTask = null;
        }
    }

    private void sendHeartbeat() {
        try {
            if (!isActive())
                return;

            session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes(UTF_8))));
            scheduleHeartbeat();
        } catch (IOException e) {
            logger.error("send heartbeat error", e);
        }
    }

    private boolean isActive() {
        return session != null && session.isOpen();
    }

    private class HeartbeatTask implements Runnable {

        private boolean expired;

        @Override
        public void run() {
            if (!this.expired && isActive()) {
                try {
                    sendHeartbeat();
                }
                catch (Throwable ex) {
                    // Ignore: already handled in sendHeartbeat ...
                }
                finally {
                    this.expired = true;
                }
            }
        }

        void cancel() {
            this.expired = true;
        }
    }

}
