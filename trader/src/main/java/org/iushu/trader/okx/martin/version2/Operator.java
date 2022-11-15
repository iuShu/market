package org.iushu.trader.okx.martin.version2;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.base.DefaultExecutor;
import org.iushu.trader.base.NotifyUtil;
import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.*;
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
import java.util.concurrent.atomic.AtomicInteger;

public class Operator implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(Operator.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Strategy<JSONObject> strategy;
    private WsJsonClient privateClient;

    private final AtomicBoolean placing = new AtomicBoolean(false);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private volatile String messageId = "";
    private final AtomicInteger failedTimes = new AtomicInteger(0);
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
                logger.info("first order has been filled, reset placing");
            }
            closeByTakeProfit();
            return;
        }

        int nextBatch = MartinOrders.instance().getBatch();
        if (this.orderBatch != nextBatch) {
            this.orderBatch = nextBatch;
            if (this.closing.get()) {
                this.closing.compareAndSet(true, false);
                logger.info("all position has been closed, reset closing");
            }
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

            JSONObject packet = PacketUtils.placeOrderPacket(first);
            this.messageId = packet.getString("id");
            this.privateClient.sendAsync(packet, r -> {
                if (r.isOK())
                    logger.info("[{}] sent first order at {} pos={} {}", this.orderBatch, latestPrice, first.getPosition(), first.getPosSide());
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
            logger.info("close by take profit {} of {} {}", takeProfitPrice, order.getPrice(), order.getPosSide());
            if (MartinOrders.instance().getOrder(order.getPosition()) != order) {   // reconfirm
                this.closing.compareAndSet(true, false);    // reset
                return;     // could be closed by other thread
            }

            for (int i = 0; i < Setting.OPERATION_MAX_FAILURE_TIMES; i++)
                if (this.closeAllPosition(order.getPosSide()))
                    return;
            this.closing.compareAndSet(true, false);    // reset for next re-try
        } catch (Exception e) {
            logger.error("close by take profit error", e);
        }
    }

    private boolean closeAllPosition(PosSide posSide) {
        if (OkxHttpUtils.closePosition(posSide)) {
            logger.info("close all position at {}", this.prices.get(this.prices.size() - 1));
            NotifyUtil.windowTips("Order Close", "close all position orders success");
            return true;
        }
        else {
            logger.error("close all position failed");
            NotifyUtil.windowTips("Order Close", "close all position orders failed");
            return false;
        }
    }

    private void checkFirstOrderPlacing(JSONObject message) {
        String op = message.getString("op");
        String id = message.getString("id");
        if (!this.messageId.equals(id) || !"order".equals(op))
            return;

        Integer code = message.getInteger("code");
        if (code != null && code == 0) {
            this.failedTimes.set(0);
            logger.info("placed order operation success");
            return;
        }

        logger.warn("place first order failed by {}", message.toJSONString());
        if (this.failedTimes.getAndIncrement() >= Setting.OPERATION_MAX_FAILURE_TIMES) {
            logger.error("placing first order failed times reached threshold, shutdown program");
            this.privateClient.shutdown();
            return;
        }
        if (this.placing.get()) {
            logger.info("reset and try place first order again");
            this.placing.compareAndSet(true, false);
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
        return this.coolingDown > 0 && this.coolingDown > System.currentTimeMillis();
    }

    public static String timestampFormat(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(dateTimeFormatter);
    }

}

