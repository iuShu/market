package org.iushu.trader;

import org.iushu.trader.base.DefaultExecutor;
import org.iushu.trader.okx.OkxPrivateWsJsonClient;
import org.iushu.trader.okx.OkxWsJsonClient;
import org.iushu.trader.okx.martin.*;
import org.iushu.trader.websocket.WsJsonClient;

import java.util.concurrent.CountDownLatch;

public class Trader {

    private final CountDownLatch control = new CountDownLatch(1);
    private volatile boolean running = false;

    private static final Trader INSTANCE = new Trader();

    private WsJsonClient wsClient = new OkxWsJsonClient();
    private WsJsonClient privateClient = new OkxPrivateWsJsonClient();

    private Trader() {
        MAStrategy strategy = new MAStrategy();
        Operator operator = new Operator(strategy);
        Authenticator authenticator = new Authenticator();
        PosListener posListener = new PosListener();

        wsClient.register(strategy);
        wsClient.register(operator);

        privateClient.register(operator);
        privateClient.register(authenticator);
        privateClient.register(posListener);
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
        DefaultExecutor.executor().shutdownNow();
        DefaultExecutor.scheduler().shutdownNow();
        control.countDown();
    }

    /**
     * add vm option before run
     *  -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
     */
    public static void main(String[] args) {
        Trader.instance().start();
    }

}
