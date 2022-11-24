package org.iushu.market;

import org.iushu.market.trade.OkxRestTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class TradingBotApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(TradingBotApplication.class);
        OkxRestTemplate bean = context.getBean(OkxRestTemplate.class);
        System.out.println(bean.getLeverage());
    }

}
