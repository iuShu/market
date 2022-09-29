package org.iushu.trader;

import org.iushu.trader.base.DefaultExecutor;
import org.iushu.trader.okx.OkxPrivateWsJsonClient;
import org.iushu.trader.okx.OkxWsJsonClient;
import org.iushu.trader.okx.martin.*;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Trader {

    private static final Logger logger = LoggerFactory.getLogger(Trader.class);

    private final CountDownLatch control = new CountDownLatch(1);
    private volatile boolean running = false;

    private static final Trader INSTANCE = new Trader();

    private WsJsonClient wsClient = new OkxWsJsonClient();
    private WsJsonClient privateClient = new OkxPrivateWsJsonClient();

    private Trader() {
        MAStrategy strategy = new MAStrategy();
        Operator operator = new Operator(strategy);
        Authenticator authenticator = new Authenticator();

        wsClient.register(strategy);
        wsClient.register(operator);

        privateClient.register(operator);
        privateClient.register(authenticator);
        privateClient.afterConnected(() -> DefaultExecutor.executor().submit(wsClient::start));
    }

    public static Trader instance() {
        return INSTANCE;
    }

    public void start() {
        if (running)
            throw new IllegalStateException("already in running");

        try {
            privateClient.start();
            running = true;
            control.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            running = false;
        }
    }

    public void stop() {
        if (!running)
            throw new IllegalStateException("not in running");

        logger.warn("trader stopping");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            // ignore
        }
        DefaultExecutor.executor().shutdown();
        DefaultExecutor.scheduler().shutdown();
        control.countDown();
        logger.warn("trader stopped");
    }

    /**
     * add vm option before run
     *  -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
     */
    public static void main(String[] args) {
        Trader.instance().start();
    }

}
