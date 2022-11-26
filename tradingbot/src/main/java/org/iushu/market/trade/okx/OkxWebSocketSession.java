package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.client.ChannelWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Wrap websocket session
 */
public class OkxWebSocketSession {

    private static final Logger logger = LoggerFactory.getLogger(OkxWebSocketSession.class);

    private final WebSocketSession session;

    public OkxWebSocketSession(WebSocketSession session) {
        this.session = session;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getSessionId() {
        return session.getId();
    }

    public boolean sendMessage(JSONObject message) {
        if (!isActive())
            return false;
        if (message.isEmpty()) {
            logger.warn("do not send blank message");
            return false;
        }

        try {
            session.sendMessage(new TextMessage(message.toJSONString()));
            return true;
        } catch (Exception e) {
            logger.error("send message error {}", message.toJSONString(), e);
            return false;
        }
    }

    public void close() {
        try {
            if (isActive())
                session.close(ChannelWebSocketHandler.INITIATE_CLOSE);
        } catch (Exception e) {
            logger.error("session close error, shutdown all", e);
            System.exit(1);
        }
    }

    public boolean isActive() {
        return session != null && session.isOpen();
    }

}
