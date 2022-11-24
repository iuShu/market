package org.iushu.market.client;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component("privateChannelHandler")
public class PrivateChannelWebSocketHandler extends ChannelWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // login
    }

}
