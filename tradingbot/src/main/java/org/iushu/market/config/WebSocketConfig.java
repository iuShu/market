package org.iushu.market.config;

import org.iushu.market.Constants;
import org.iushu.market.client.ChannelWebSocketHandler;
import org.iushu.market.client.TradingWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;

@Configuration
public class WebSocketConfig {

    private TradingProperties properties;
    private TradingProperties.ApiInfo apiInfo;
    private WebSocketProperties webSocketProperties;
    private TaskScheduler taskScheduler;
    private AsyncListenableTaskExecutor taskExecutor;

    @Bean
    public WebSocketClient publicClient() {
        TradingWebSocketClient webSocketClient = new TradingWebSocketClient(taskScheduler, properties);
        webSocketClient.setTaskExecutor(taskExecutor);
//        webSocketClient.doHandshake(channelHandler(), this.apiInfo.getWsPublicUrl());
        return webSocketClient;
    }

    @Bean
    public WebSocketHandler channelHandler() {
        return new ChannelWebSocketHandler(webSocketProperties);
    }

    @Bean
    @Profile(Constants.EXChANGE_OKX)
    public WebSocketClient privateClient() {
        TradingWebSocketClient webSocketClient = new TradingWebSocketClient(taskScheduler, properties);
        webSocketClient.setTaskExecutor(taskExecutor);
//        webSocketClient.doHandshake(privateChannelHandler(), this.apiInfo.getWsPrivateUrl());
        return webSocketClient;
    }

    @Bean("privateChannelHandler")
    @Profile(Constants.EXChANGE_OKX)
    public WebSocketHandler privateChannelHandler() {
        return new ChannelWebSocketHandler(webSocketProperties);
    }

    @Autowired
    public void setProperties(TradingProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setApiInfo(TradingProperties.ApiInfo apiInfo) {
        this.apiInfo = apiInfo;
    }

    @Autowired
    public void setWebSocketProperties(WebSocketProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties;
    }

    @Autowired
    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Autowired
    public void setTaskExecutor(AsyncListenableTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

}
