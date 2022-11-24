package org.iushu.market.config;

import org.iushu.market.Constants;
import org.springframework.beans.BeansException;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.task.TaskSchedulerCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableAsync
@EnableWebSocket
@EnableScheduling
@Configuration
public class ApplicationConfig {

    @Bean
    public TaskScheduler taskScheduler(TaskSchedulerBuilder builder) {
        return builder.build();
    }

    @Bean
    @Profile({"prod", Constants.EXChANGE_OKX})
    public RestTemplate restTemplate(RestTemplateBuilder builder, TradingProperties.ApiInfo apiInfo) {
        return builder.defaultHeader("OK-ACCESS-KEY", apiInfo.getApiKey())
                .defaultHeader("OK-ACCESS-PASSPHRASE", apiInfo.getPassphrase())
                .defaultHeader("Content-Type", "application/json").build();
    }

    @Bean
    @Profile({"test", Constants.EXChANGE_OKX})
    public RestTemplate testRestTemplate(RestTemplateBuilder builder, TradingProperties.ApiInfo apiInfo) {
        return builder.defaultHeader("OK-ACCESS-KEY", apiInfo.getApiKey())
                .defaultHeader("OK-ACCESS-PASSPHRASE", apiInfo.getPassphrase())
                .defaultHeader("x-simulated-trading", "1")
                .defaultHeader("Content-Type", "application/json").build();
    }

}
