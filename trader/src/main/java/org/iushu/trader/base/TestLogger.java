package org.iushu.trader.base;

import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class TestLogger {

    private static final Logger logger = LoggerFactory.getLogger(TestLogger.class);

    public static void main(String[] args) {
        // log4j2 use ASYNC mode
        // 1. not recommended (not affective)
        System.setProperty("log4j2.contextSelector", AsyncLoggerContextSelector.class.getName());
        // 2. recommended
        // add vm option -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector

        logger.trace(">> trace");
        logger.debug(">> debug");
        logger.info(">> info");
        logger.warn(">> warn");
        logger.error(">> error");

        new Thread(() -> {
            long start = System.currentTimeMillis();
            Random random = new Random();
            for (int i = 0; i < 1000000; i++)
                logger.info("test {} {}", random.nextInt(500000), random.nextDouble());
            System.out.println("cost: " + (System.currentTimeMillis() - start));
        }).start();

        System.out.println(logger.getClass().getName());
        System.out.println("main end");
    }

}
