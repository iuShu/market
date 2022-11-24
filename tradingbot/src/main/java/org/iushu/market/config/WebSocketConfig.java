package org.iushu.market.config;

import org.iushu.market.client.TradingWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;

@Configuration
public class WebSocketConfig {

    private TradingProperties properties;
    private TradingProperties.ApiInfo apiInfo;
    private TaskScheduler taskScheduler;

    @Autowired
    public void setProperties(TradingProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setApiInfo(TradingProperties.ApiInfo apiInfo) {
        this.apiInfo = apiInfo;
    }

    @Autowired
    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Bean
    public WebSocketClient publicClient(@Qualifier("publicChannelHandler") WebSocketHandler webSocketHandler) {
        WebSocketClient webSocketClient = new TradingWebSocketClient(taskScheduler, properties);
//        webSocketClient.doHandshake(webSocketHandler, this.apiInfo.getWsPublicUrl());
        return webSocketClient;
    }

    @Bean
    public WebSocketClient privateClient(@Qualifier("privateChannelHandler") WebSocketHandler webSocketHandler) {
        WebSocketClient webSocketClient = new TradingWebSocketClient(taskScheduler, properties);
//        webSocketClient.doHandshake(webSocketHandler, this.apiInfo.getWsPrivateUrl());
        return webSocketClient;
    }

}
