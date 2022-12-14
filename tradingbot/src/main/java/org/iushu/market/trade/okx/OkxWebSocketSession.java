package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.client.ChannelWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrap websocket session
 */
public class OkxWebSocketSession {

    private static final Logger logger = LoggerFactory.getLogger(OkxWebSocketSession.class);

    private volatile WebSocketSession publicSession;
    private volatile WebSocketSession privateSession;

    public WebSocketSession getPublicSession() {
        return publicSession;
    }

    public void setPublicSession(WebSocketSession publicSession) {
        this.publicSession = publicSession;
    }

    public WebSocketSession getPrivateSession() {
        return privateSession;
    }

    public void setPrivateSession(WebSocketSession privateSession) {
        this.privateSession = privateSession;
    }

    public boolean sendPublicMessage(JSONObject message) {
        return sendMessage(publicSession, message);
    }

    public boolean sendPrivateMessage(JSONObject message) {
        return sendMessage(privateSession, message);
    }

    private boolean sendMessage(WebSocketSession session, JSONObject message) {
        if (!isActive(session))
            return false;
        if (message.isEmpty()) {
            logger.warn("do not send blank message");
            return false;
        }

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message.toJSONString()));
            }
            return true;
        } catch (Exception e) {
            logger.error("send message error {}", message.toJSONString(), e);
            return false;
        }
    }

    public void close() {
        try {
            if (isActive(publicSession))
                publicSession.close(ChannelWebSocketHandler.INITIATE_CLOSE);
            if (isActive(privateSession))
                privateSession.close(ChannelWebSocketHandler.INITIATE_CLOSE);
        } catch (Exception e) {
            logger.error("session close error, shutdown all", e);
            System.exit(1);
        }
    }

    public boolean isActive(WebSocketSession session) {
        return session != null && session.isOpen();
    }

}
