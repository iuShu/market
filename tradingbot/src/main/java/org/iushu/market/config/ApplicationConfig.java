package org.iushu.market.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.iushu.market.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.task.TaskSchedulerCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
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

import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

@EnableAsync
@EnableWebSocket
@EnableScheduling
@EnableEncryptableProperties
@Configuration
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Bean
    @Primary
    public TaskExecutor taskExecutor(TaskExecutorBuilder builder) {
        TaskExecutorCustomizer customizer = (executor) -> executor.setRejectedExecutionHandler((r, e) -> logger.warn("executor rejected task {}", r));
        return builder.corePoolSize(Runtime.getRuntime().availableProcessors() + 1).customizers(customizer).build();
    }

    @Bean
    public TaskScheduler taskScheduler(TaskSchedulerBuilder builder) {
        TaskSchedulerCustomizer customizer = (executor) -> executor.setRejectedExecutionHandler((r, e) -> logger.warn("scheduler rejected task {}", r));
        return builder.customizers(customizer).build();
    }

    @Bean
    @Profile(Constants.EXChANGE_OKX)
    public RestTemplate testRestTemplate(RestTemplateBuilder builder) {
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

    @EventListener(ContextRefreshedEvent.class)
    public void onReady(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        SimpleApplicationEventMulticaster multicaster = context.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, SimpleApplicationEventMulticaster.class);
        TaskExecutor executor = context.getBean(TaskExecutor.class);
        multicaster.setTaskExecutor(executor);
    }

}
