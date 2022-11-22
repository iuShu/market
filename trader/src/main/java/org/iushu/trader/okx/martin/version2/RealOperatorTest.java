package org.iushu.trader.okx.martin.version2;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.OkxWsJsonClient;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.okx.martin.Order;
import org.iushu.trader.okx.martin.Strategy;
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

public class RealOperatorTest implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RealOperatorTest.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Strategy<JSONObject> strategy;
    private WsJsonClient client;

    private final AtomicBoolean placing = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private volatile long coolingDown = 0L;
    private volatile long displayInterval = 0L;
    private volatile int orderBatch;

    private final List<Double> prices = new CopyOnWriteArrayList<>();

    public RealOperatorTest(Strategy<JSONObject> strategy) {
        this.strategy = strategy;
        this.orderBatch = MartinOrders.instance().getBatch();
    }

    @Override
    public JSONObject publicChannel() {
        return Setting.CHANNEL_TICKERS;
    }

    @Override
    public void setClient(WsJsonClient client) {
        this.client = client;
    }

    @Override
    public void consume(JSONObject message) {
        if (message.containsKey("op")) {
            checkFirstOrderPlacing(message);
            return;
        }

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
            checkNextOrderFilled(price);
            closeByTakeProfit();
            return;
        }

        int nextBatch = MartinOrders.instance().getBatch();
        if (this.orderBatch != nextBatch) {
            this.orderBatch = nextBatch;
            coolingDown();
        }

        PosSide posSide = strategy.decideSide(ticker);
        if (posSide == null)
            return;

        MartinOrders.instance().setPosSide(this.orderBatch, posSide);
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

            logger.info("[{}] placed first order {} {} {}", this.orderBatch, first.getPosition(), latestPrice, first.getPosSide());
            first.setPrice(latestPrice);
            MartinOrders.instance().calcOrdersPrice();
            filledOrder(first, latestPrice);
            MartinOrders.instance().allOrders().stream().filter(order -> order.getPosition() != first.getPosition())
                .forEach(order -> logger.info("[{}] placed follow order with {} {}", this.orderBatch, order.getPosition(), order.getPrice()));
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
//        debugPriceCheck(takeProfitPrice);
        for (Double price : this.prices) {
            if (!order.getPosSide().isProfit(takeProfitPrice, price))
                return;
        }

        if (!this.closing.compareAndSet(false, true))
            return;
        try {
            logger.info("close by take profit {} of {} at pos={}", takeProfitPrice, order.getPrice(), order.getPosition());
            if (MartinOrders.instance().getOrder(order.getPosition()) != order)  // reconfirm
                return;     // could be closed by other thread

            logger.info("placed close orders");
            logger.info("sent cancel orders");

            MartinOrders.instance().reset();
            logger.warn("*** close all position at {} ***", this.prices.get(this.prices.size() - 1));
        } catch (Exception e) {
            logger.error("close by take profit error", e);
        } finally {
            closing.compareAndSet(true, false);
        }
    }

    private void checkFirstOrderPlacing(JSONObject message) {
        if (message.getIntValue("code") == 0)
            return;

        logger.warn("place first order failed by {}", message.toJSONString());
        if (this.placing.get()) {
            logger.info("reset and try place first order again");
            this.placing.compareAndSet(true, false);
        }
    }

    private void checkNextOrderFilled(double price) {
        Order current = MartinOrders.instance().current();
        Order order = MartinOrders.instance().getOrder(current.getPosition() * 2);
        double stopLossPrice = order == null ? MartinOrders.instance().stopLossPrice() : order.getPrice();
        if (current.getPosSide().isLoss(stopLossPrice, price)) {
            if (order != null)
                filledOrder(order, price);
            else {
                MartinOrders.instance().reset();
                logger.warn("[{}] Martin FAILED at px={} with last={}, {}", this.orderBatch, price, current.getPosition(), current.getPrice());
                this.client.shutdown();
            }
        }
    }

    private void filledOrder(Order filled, double filledPrice) {
        if (filled.getState().equals(Constants.ORDER_STATE_FILLED))
            return;

        MartinOrders instance = MartinOrders.instance();
        instance.setCurrent(filled);
        filled.setOrderId(Long.toString(System.currentTimeMillis()));
        filled.setState(Constants.ORDER_STATE_FILLED);
        filled.setPrice(filledPrice);
        logger.info("[{}] order has been filled {} {} tp={}, sl={}", this.orderBatch, filled.getPosition(),
                filledPrice, instance.takeProfitPrice(filled), instance.stopLossPrice());
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
        return this.coolingDown > 0 && this.coolingDown > System.currentTimeMillis();
    }

    public static String timestampFormat(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(dateTimeFormatter);
    }

}

