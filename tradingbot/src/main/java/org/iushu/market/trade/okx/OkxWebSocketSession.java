package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Wrap websocket session
 */
public class OkxWebSocketSession {

    private static final Logger logger = LoggerFactory.getLogger(OkxWebSocketSession.class);

    private WebSocketSession session;

    public OkxWebSocketSession(WebSocketSession session) {
        this.session = session;
    }

    public boolean sendMessage(JSONObject message) {
        if (!isActive())
            return false;

        try {
            session.sendMessage(JsonMessage.of(message));
            return true;
        } catch (IOException e) {
            logger.error("send message error {}", message.toJSONString(), e);
            return false;
        }
    }

    public void close() {
        try {
            if (isActive())
                session.close();
        } catch (Exception e) {
            logger.error("session close error, shutdown all", e);
            System.exit(1);
        }
    }

    public boolean isActive() {
        return session != null && session.isOpen();
    }

}
