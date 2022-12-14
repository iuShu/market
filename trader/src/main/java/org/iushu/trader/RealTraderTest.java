package org.iushu.trader;

import org.iushu.trader.base.DefaultExecutor;
import org.iushu.trader.okx.OkxPrivateWsJsonClient;
import org.iushu.trader.okx.OkxWsJsonClient;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.okx.martin.version2.RealOperatorTest;
import org.iushu.trader.okx.martin.version2.EMAStrategy;
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

    private RealTraderTest() {
        logger.info("running at {} env", Setting.ENV.toUpperCase());

        EMAStrategy strategy = new EMAStrategy();
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
        this.wsClient.shutdown();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            // ignore
        }
        DefaultExecutor.executor().shutdown();
        DefaultExecutor.scheduler().shutdown();
        logger.debug("trader count {}", control.getCount());
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
