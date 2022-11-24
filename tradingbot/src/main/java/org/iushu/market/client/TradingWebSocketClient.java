package org.iushu.market.client;

import org.iushu.market.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TradingWebSocketClient extends StandardWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(TradingWebSocketClient.class);

    private TaskScheduler taskScheduler;
    private TradingProperties properties;

    private WebSocketSession session;
    private ScheduledFuture<?> heartbeatFuture;
    private TradingWebSocketClient.HeartbeatTask heartbeatTask;

    public TradingWebSocketClient(TaskScheduler taskScheduler, TradingProperties properties) {
        Assert.notNull(taskScheduler, "required task scheduler");
        Assert.notNull(properties, "required trading properties");
        this.taskScheduler = taskScheduler;
        this.properties = properties;
    }

    @Override
    public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVars) {
        ChannelWebSocketHandler channelWebSocketHandler = (ChannelWebSocketHandler) webSocketHandler;
        channelWebSocketHandler.setClient(this);
        channelWebSocketHandler.setWebsocketUrl(uriTemplate);
        ListenableFuture<WebSocketSession> doHandshake = super.doHandshake(webSocketHandler, uriTemplate, uriVars);
        Runnable afterConnected = () -> {
            try {
                this.session = doHandshake.get();
                scheduleHeartbeat();
            } catch (Exception e) {
                logger.error("after connected task error", e);
            }
        };
        AsyncListenableTaskExecutor taskExecutor = getTaskExecutor();
        if (taskExecutor != null)
            taskExecutor.submit(afterConnected);
        else
            afterConnected.run();
        return doHandshake;
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
