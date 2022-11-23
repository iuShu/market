package org.iushu.market.config;

import org.iushu.market.client.ChannelWebSocketHandler;
import org.iushu.market.client.PrivateChannelWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class WebSocketConfig {

    private final TradingProperties.ApiInfo apiInfo;

    public WebSocketConfig(TradingProperties.ApiInfo apiInfo) {
        this.apiInfo = apiInfo;
    }

    @Bean
    public WebSocketClient publicClient() {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
//        webSocketClient.doHandshake(publicHandler(), this.apiInfo.getWsPublicUrl());
        return webSocketClient;
    }

    @Bean
    public WebSocketClient privateClient() {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
//        webSocketClient.doHandshake(privateHandler(), this.apiInfo.getWsPrivateUrl());
        return webSocketClient;
    }

//    @Bean
//    public WebSocketHandler publicHandler() {
//        return new ChannelWebSocketHandler();
//    }

    @Bean
    public WebSocketHandler privateHandler() {
        return new PrivateChannelWebSocketHandler();
    }

}
