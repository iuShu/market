package org.iushu.market.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableAsync
@EnableWebSocket
@EnableScheduling
@Configuration
public class ApplicationConfig {


}
