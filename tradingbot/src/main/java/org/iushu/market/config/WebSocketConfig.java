package org.iushu.market.config;

import org.iushu.market.Constants;
import org.iushu.market.client.ChannelWebSocketHandler;
import org.iushu.market.client.TradingWebSocketClient;
import org.iushu.market.component.MultiProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;

import java.util.Map;

@Configuration
public class WebSocketConfig {

    private TradingProperties properties;
    private TradingProperties.ApiInfo apiInfo;
    private WebSocketProperties webSocketProperties;
    private TaskScheduler taskScheduler;
    private TaskExecutor taskExecutor;

    @Bean
    public WebSocketClient publicClient() {
        TradingWebSocketClient webSocketClient = new TradingWebSocketClient();
        webSocketClient.setWsUrl(apiInfo.getWsPublicUrl());
        webSocketClient.setProperties(properties);
        webSocketClient.setTaskScheduler(taskScheduler);
        webSocketClient.setTaskExecutor((AsyncListenableTaskExecutor) taskExecutor);
        webSocketClient.setWebSocketHandler(channelHandler());
        return webSocketClient;
    }

    @Bean
    public WebSocketHandler channelHandler() {
        return new ChannelWebSocketHandler(webSocketProperties);
    }

    @Bean
    @MultiProfile({Constants.EXChANGE_OKX, "test"})
    public WebSocketClient privateClient() {
        TradingWebSocketClient webSocketClient = new TradingWebSocketClient();
        webSocketClient.setWsUrl(apiInfo.getWsPrivateUrl());
        webSocketClient.setProperties(properties);
        webSocketClient.setTaskScheduler(taskScheduler);
        webSocketClient.setTaskExecutor((AsyncListenableTaskExecutor) taskExecutor);
        webSocketClient.setWebSocketHandler(privateChannelHandler());
        return webSocketClient;
    }

    @Bean("privateChannelHandler")
    @MultiProfile({Constants.EXChANGE_OKX, "test"})
    public WebSocketHandler privateChannelHandler() {
        return new ChannelWebSocketHandler(webSocketProperties);
    }

    /**
     * other action can be processed before websocket connected, by listening ContextRefreshedEvent
     * @see org.springframework.context.event.ContextRefreshedEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void connect(ApplicationReadyEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        Map<String, TradingWebSocketClient> clients = context.getBeansOfType(TradingWebSocketClient.class);
        clients.values().forEach(TradingWebSocketClient::doHandshake);
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
    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

}
