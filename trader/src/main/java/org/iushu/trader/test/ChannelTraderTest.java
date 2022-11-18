package org.iushu.trader.test;

import org.iushu.trader.okx.OkxPrivateWsJsonClient;
import org.iushu.trader.websocket.WsJsonClient;

import java.util.concurrent.CountDownLatch;

public class ChannelTraderTest {

    private static boolean running = false;
    private final static CountDownLatch control = new CountDownLatch(1);
    private final static WsJsonClient client = new OkxPrivateWsJsonClient();

    static {
//        client.register(new AlgoChannelTest());
        client.register(new OrdersChannelTest());
    }

    static void start() {
        if (running)
            throw new IllegalStateException("already running");

        try {
            client.start();
            control.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            running = false;
        }
    }

    public static void main(String[] args) {
        ChannelTraderTest.start();
    }

}
