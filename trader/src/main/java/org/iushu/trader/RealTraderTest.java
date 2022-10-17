package org.iushu.trader;

import org.iushu.trader.base.DefaultExecutor;
import org.iushu.trader.okx.OkxPrivateWsJsonClient;
import org.iushu.trader.okx.OkxWsJsonClient;
import org.iushu.trader.okx.martin.Authenticator;
import org.iushu.trader.okx.martin.MAStrategy;
import org.iushu.trader.okx.martin.Operator;
import org.iushu.trader.okx.martin.RealOperatorTest;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RealTraderTest {

    private static final Logger logger = LoggerFactory.getLogger(RealTraderTest.class);

    private final CountDownLatch control = new CountDownLatch(1);
    private volatile boolean running = false;

    private static final RealTraderTest INSTANCE = new RealTraderTest();

    private WsJsonClient wsClient = new OkxWsJsonClient();
    private OkxPrivateWsJsonClient privateClient = new OkxPrivateWsJsonClient();

    private RealTraderTest() {
        MAStrategy strategy = new MAStrategy();
        RealOperatorTest operator = new RealOperatorTest(strategy);

        wsClient.register(strategy);
        wsClient.register(operator);

//        privateClient.register(operator);
//        privateClient.afterLogin((c) -> DefaultExecutor.executor().submit(wsClient::start));
    }

    public static RealTraderTest instance() {
        return INSTANCE;
    }

    public void start() {
        if (running)
            throw new IllegalStateException("already in running");

        try {
            running = true;
            wsClient.start();
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
        if (control.getCount() > 0)
            control.countDown();
        logger.warn("trader stopped");
    }

    /**
     * add vm option before run
     *  -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
     */
    public static void main(String[] args) {
        RealTraderTest.instance().start();
    }

}
