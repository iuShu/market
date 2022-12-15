package org.iushu.market.client;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * wrapped WebSocketSession for control message sending action
 */
public class WrapWebSocketSession implements WebSocketSession {

    private final WebSocketSession wrapped;
    private final Lock sendLock = new ReentrantLock();

    public WrapWebSocketSession(WebSocketSession session) {
        this.wrapped = session;
    }

    @Override
    public String getId() {
        return wrapped.getId();
    }

    @Override
    public URI getUri() {
        return wrapped.getUri();
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
        return wrapped.getHandshakeHeaders();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return wrapped.getAttributes();
    }

    @Override
    public Principal getPrincipal() {
        return wrapped.getPrincipal();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return wrapped.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return wrapped.getRemoteAddress();
    }

    @Override
    public String getAcceptedProtocol() {
        return wrapped.getAcceptedProtocol();
    }

    @Override
    public void setTextMessageSizeLimit(int messageSizeLimit) {
        wrapped.setTextMessageSizeLimit(messageSizeLimit);
    }

    @Override
    public int getTextMessageSizeLimit() {
        return wrapped.getTextMessageSizeLimit();
    }

    @Override
    public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        wrapped.setBinaryMessageSizeLimit(messageSizeLimit);
    }

    @Override
    public int getBinaryMessageSizeLimit() {
        return wrapped.getBinaryMessageSizeLimit();
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
        return wrapped.getExtensions();
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
        sendLock.lock();
        try {
            wrapped.sendMessage(message);
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public boolean isOpen() {
        return wrapped.isOpen();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public void close(CloseStatus status) throws IOException {
        wrapped.close(status);
    }
}
