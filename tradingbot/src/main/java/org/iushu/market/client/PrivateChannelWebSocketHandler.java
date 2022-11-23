package org.iushu.market.client;

import org.springframework.web.socket.WebSocketSession;

public class PrivateChannelWebSocketHandler extends ChannelWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // login
    }
}
