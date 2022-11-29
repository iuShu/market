package org.iushu.market.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.iushu.market.Constants;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.net.ssl.SSLContext;

import static org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;
import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

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
    @Profile({"test", Constants.EXChANGE_OKX})
    public RestTemplate testRestTemplate(RestTemplateBuilder builder, TradingProperties.ApiInfo apiInfo) {
        try {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
                    .setConnectionRequestTimeout(7000).setSocketTimeout(10000).build();
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            HttpClient httpsClient = HttpClients.custom().setSSLSocketFactory(factory).setDefaultRequestConfig(requestConfig).build();
            ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpsClient);
            return builder.requestFactory(() -> requestFactory).build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        SimpleApplicationEventMulticaster multicaster = context.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, SimpleApplicationEventMulticaster.class);
        TaskExecutor executor = context.getBean(APPLICATION_TASK_EXECUTOR_BEAN_NAME, TaskExecutor.class);
        multicaster.setTaskExecutor(executor);
    }

}
