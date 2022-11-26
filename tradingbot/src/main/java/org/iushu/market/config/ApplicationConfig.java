package org.iushu.market.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.iushu.market.Constants;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.net.ssl.SSLContext;

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
        try {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
                    .setConnectionRequestTimeout(7000).setSocketTimeout(10000).build();
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            HttpClient httpsClient = HttpClients.custom().setSSLSocketFactory(factory).setDefaultRequestConfig(requestConfig).build();
            ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpsClient);
            return builder.requestFactory(() -> requestFactory)
                    .defaultHeader("OK-ACCESS-KEY", apiInfo.getApiKey())
                    .defaultHeader("OK-ACCESS-PASSPHRASE", apiInfo.getPassphrase())
                    .defaultHeader("x-simulated-trading", "1")
                    .defaultHeader("Content-Type", "application/json;charset=utf-8").build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

}
