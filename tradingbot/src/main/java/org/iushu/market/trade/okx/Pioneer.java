package org.iushu.market.trade.okx;

import org.iushu.market.client.ChannelWebSocketHandler;
import org.iushu.market.client.event.ChannelOpenedEvent;
import org.iushu.market.config.TradingProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

@Component
public class Pioneer {

    private final TradingProperties.ApiInfo apiInfo;

    public Pioneer(TradingProperties.ApiInfo apiInfo) {
        this.apiInfo = apiInfo;
    }

    @Async
    @EventListener(ChannelOpenedEvent.class)
    public void channelLogin(ChannelOpenedEvent<StandardWebSocketSession> event) {
        ChannelWebSocketHandler handler = (ChannelWebSocketHandler) event.getSource();
        StandardWebSocketSession session = event.getPayload();
        if (apiInfo.getWsPrivateUrl().equals(handler.getWebsocketUrl())) {
            // login message
        }
        else {
            // subscribe channel
        }
    }

}
