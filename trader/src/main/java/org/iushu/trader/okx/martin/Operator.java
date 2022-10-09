package org.iushu.trader.okx.martin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.okx.*;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Operator implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(Operator.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Strategy<JSONObject> strategy;
    private WsJsonClient privateClient;

    private final AtomicBoolean placing = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private volatile long coolingDown = 0L;
    private volatile long displayInterval = 0L;
    private volatile int orderBatch;

    private final List<Double> prices = new CopyOnWriteArrayList<>();

    public Operator(Strategy<JSONObject> strategy) {
        this.strategy = strategy;
        this.orderBatch = MartinOrders.instance().getBatch();
    }

    @Override
    public JSONObject publicChannel() {
        return Setting.CHANNEL_TICKERS;
    }

    @Override
    public void setPrivateClient(WsJsonClient client) {
        this.privateClient = client;
    }

    @Override
    public void consume(JSONObject message) {
        JSONArray data = message.getJSONArray(OkxWsJsonClient.KEY_DATA);
        if (data == null || data.isEmpty())
            return;

        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");
        this.prices.add(price);
        if (this.prices.size() > Setting.ORDER_CLOSE_PX_THRESHOLD)
            this.prices.remove(0);

        Order first = MartinOrders.instance().first();
        if (MartinOrders.instance().validOrder(first)) {
            if (this.placing.get()) {
                this.placing.compareAndSet(true, false);
                logger.info("first order has been filled, reset state");
            }
            closeByTakeProfit();
            return;
        }

        int nextBatch = MartinOrders.instance().getBatch();
        if (this.orderBatch != nextBatch) {
            this.orderBatch = nextBatch;
            coolingDown();
        }

        if (strategy.satisfy(ticker))
            placeFirstOrder(price);
    }

    private void placeFirstOrder(double latestPrice) {
        if (isCoolingDown() || !this.placing.compareAndSet(false, true))
            return;

        try {
            Order first = MartinOrders.instance().first();
            if (first == null || first.getOrderId() != null) {
                logger.error("unexpected first order state {}", first);
                return;
            }

            JSONObject packet = PacketUtils.placeOrderPacket(first);
            this.privateClient.sendAsync(packet, r -> {
                if (r.isOK())
                    logger.info("sent [{}]first order at {} pos={}", this.orderBatch, latestPrice, first.getPosition());
                else
                    logger.error("send first order failed", r.getException());
            });
        } catch (Exception e) {
            logger.error("place first order error", e);
        }
    }

    private void closeByTakeProfit() {
        if (this.prices.size() < Setting.ORDER_CLOSE_PX_THRESHOLD)
            return;

        Order order = MartinOrders.instance().current();
        if (order == null || !Constants.ORDER_STATE_FILLED.equals(order.getState()))
            return;

        double takeProfitPrice = MartinOrders.instance().takeProfitPrice(order);
        debugPriceCheck(takeProfitPrice);
        for (Double price : this.prices) {
            if (!order.getPosSide().isProfit(takeProfitPrice, price))
                return;
        }

        if (!this.closing.compareAndSet(false, true))
            return;
        try {
            logger.info("close by take profit {} of {}", takeProfitPrice, order.getPrice());
            if (MartinOrders.instance().getOrder(order.getPosition()) != order)  // reconfirm
                return;     // could be closed by other thread

            JSONObject packet = PacketUtils.cancelOrdersPacket();
            this.privateClient.sendAsync(packet, r -> {
                if (r.isOK())
                    logger.info("sent cancel orders");
                else
                    logger.error("send cancel orders failed", r.getException());
            });
            if (!OkxHttpUtils.closePosition(order.getPosSide())) {
                logger.error("close all position failed");
                return;
            }

            MartinOrders.instance().reset();
            logger.info("*** close all position at {} ***", this.prices.get(this.prices.size() - 1));
        } catch (Exception e) {
            logger.error("close by take profit error", e);
        } finally {
            closing.compareAndSet(true, false);
        }
    }

    private void debugPriceCheck(double takeProfitPrice) {
        long now = System.currentTimeMillis();
        if (this.displayInterval < now) {
            logger.debug("check tp px {} at {}", takeProfitPrice, this.prices);
            this.displayInterval = now + Setting.CANDLE_TYPE_MILLISECONDS;
        }
    }

    private void coolingDown() {
        this.coolingDown = System.currentTimeMillis() + Setting.COOLING_DOWN_MILLISECONDS;
        logger.info("cooling down till to {}", timestampFormat(this.coolingDown));
    }

    private boolean isCoolingDown() {
        return this.coolingDown < System.currentTimeMillis();
    }

    public static String timestampFormat(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(dateTimeFormatter);
    }

}

