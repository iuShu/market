package org.iushu.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@SpringBootApplication
public class TradingBotApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(TradingBotApplication.class);
//        OkxRestTemplate bean = context.getBean(OkxRestTemplate.class);
//        System.out.println(bean.getLeverage());
//        Object bean = context.getBean("taskExecutor");
//        System.out.println(bean.getClass().getName());
//        Map<String, Object> beans = context.getBeansWithAnnotation(Component.class);
//        beans.keySet().forEach(System.out::println);
//        beans.values().forEach(te -> System.out.println(te.getClass().getName()));
    }

}
