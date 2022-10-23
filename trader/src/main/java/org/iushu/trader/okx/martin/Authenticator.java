package org.iushu.trader.okx.martin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.base.NotifyUtil;
import org.iushu.trader.okx.OkxHttpUtils;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.PacketUtils;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Authenticator implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);

    private WsJsonClient client;
    private final Semaphore operating = new Semaphore(1);
    private volatile String messageId = "";
    private volatile AtomicInteger failedTimes = new AtomicInteger(0);

    @Override
    public JSONObject privateChannel() {
        return Setting.CHANNEL_ORDERS;
    }

    @Override
    public void setPrivateClient(WsJsonClient client) {
        this.client = client;
    }

    @Override
    public void consume(JSONObject message) {
        if (message.containsKey("op")) {
            checkOperationResult(message);
            return;
        }

        JSONArray data = message.getJSONArray("data");
        JSONObject order = data.getJSONObject(0);
        String ordId = order.getString("ordId");
        Integer position = order.getInteger("sz");
        String state = order.getString("state");
        String side = order.getString("side");
        long update = order.getLong("uTime");

        double fillSz = order.getDoubleValue("fillSz");
        if (fillSz > 0 && fillSz < position)
            logger.debug("order {} not fulfilled {}", position, fillSz);

        MartinOrders martinOrders = MartinOrders.instance();
        switch (state) {
            case Constants.ORDER_STATE_LIVE:
                Order plain = martinOrders.getOrder(position);
                if (plain == null) {
//                    logger.warn("unknown/close plain order {}", message.toJSONString());
                    return;     // placed close all position or other unknown situation
                }

                if (plain.getOrderId() != null)     // position partial filled
                    return;
                plain.setOrderId(ordId);
                plain.setCreateTime(order.getLong("cTime"));
                plain.setUpdateTime(update);
                logger.info("placed order {} pos={}", ordId, position);
                break;
            case Constants.ORDER_STATE_FILLED:
                if (Setting.SIDE_CLOSE.equals(side)) {
                    logger.warn("received close order");
                    cancelAllPendingOrders();
                    return;
                }

                Order live = martinOrders.getOrder(position);
                if (live == null) {
                    logger.warn("unknown/close filled order {}", message.toJSONString());
                    throw new IllegalStateException(ordId + " has been filled unexpected with pos=" + position);
                }
                martinOrders.setCurrent(live);
                live.setState(state);
                live.setPrice(order.getDoubleValue("fillPx"));
                live.setUpdateTime(update);
                logger.info("filled order {} pos={} at {}", ordId, position, live.getPrice());

                if (position == Setting.ORDER_START_POS) {
                    MartinOrders.instance().calcOrdersPrice();
                    placeAllNext();
                }
                NotifyUtil.windowTipsAndVoice("Order Filled", "Order filled, price " + live.getPrice() + ", position " + position);
                break;
            case Constants.ORDER_STATE_CANCELED:
                logger.info("canceled order {} pos={}", ordId, position);
                Order filled = martinOrders.getOrder(position);
                if (filled == null || Setting.SIDE_OPEN.equals(side)) {
//                    logger.warn("canceled order not found, {} ", message.toJSONString());
                    return;
                }
                filled.setState(state);
                filled.setUpdateTime(update);
                break;
        }
    }

    private void placeAllNext() {
        acquireSemaphore();
        try {
            Collection<Order> orders = MartinOrders.instance().allOrders();
            orders.removeIf(o -> o.getPosition() == Setting.ORDER_START_POS);
            JSONObject packet = PacketUtils.placeOrdersPacket(orders);
            this.messageId = packet.getString("id");
            this.client.sendAsync(packet, r -> {
                if (r.isOK()) {
                    logger.info("sent {} next orders", orders.size());
                }
                else {
                    logger.error("send {} next orders failed", orders.size());
                    this.client.shutdown();
                }
                operating.release();
            });
        } catch (Exception e) {
            logger.error("place all next error", e);
            this.client.shutdown();
            operating.release();
        }
    }

    private void addExtraMargin(Order order) {
        if (!Constants.ORDER_STATE_FILLED.equals(order.getState()))
            return;

        double extra = MartinOrders.instance().extraMarginBalance(order);
        if (extra <= 0) {
            logger.info("enough margin balance in order {} {} pos={}",
                    order.getOrderId(), order.getState(), order.getPosition());
            return;
        }

        acquireSemaphore();
        try {
            logger.debug("ready to add extra {} margin for {}", extra, order);
            if (order != MartinOrders.instance().getOrder(order.getPosition()))
                return;     // could be closed by other thread

            if (OkxHttpUtils.addExtraMargin(order.getPosSide(), extra)) {
                logger.info("added extra margin {} for pos={}", extra, order.getPosition());
            }
            else {
                logger.error("add extra margin failed for pos={} {}", order.getPosition(), extra);
                this.client.shutdown();
            }
        } catch (Exception e) {
            logger.error("add extra margin error", e);
            this.client.shutdown();
        } finally {
            operating.release();
        }
    }

    private void cancelAllPendingOrders() {
        acquireSemaphore();
        try {
            Collection<Order> orders = MartinOrders.instance().allOrders();
            List<Order> lives = orders.stream().filter(o -> Constants.ORDER_STATE_LIVE.equals(o.getState())).collect(Collectors.toList());
            if (lives.isEmpty()) {
                logger.warn("all orders[{}] has been filled", MartinOrders.instance().getBatch());
                return;
            }

            JSONObject packet = PacketUtils.cancelOrdersPacket(lives);
            this.messageId = packet.getString("id");
            this.client.sendAsync(packet, r -> {
                if (r.isOK())
                    logger.info("sent cancel orders");
                else {
                    logger.error("send cancel orders failed", r.getException());
                    NotifyUtil.windowTips("Order Close", "sent cancel orders failed by " + r.getException().getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("send all position failed", e);
        } finally {
            operating.release();
        }
    }

    private void checkOperationResult(JSONObject message) {
        String op = message.getString("op");
        String id = message.getString("id");
        if (id == null || !id.equals(this.messageId))
            return;

        Integer code = message.getInteger("code");
        if (code != null && code == 0) {
            this.failedTimes.set(0);
            logger.info("{} operation success", op);
            if (!op.equals("batch-cancel-orders"))
                return;

            MartinOrders.instance().reset();
            NotifyUtil.windowTipsAndVoice("Order Close", "Order batch " + MartinOrders.instance().getBatch() + " closed.");
        }
        else {
            logger.error("{} operation failed by {}", op, message);
            if (this.failedTimes.getAndIncrement() >= Setting.OPERATION_MAX_FAILURE_TIMES) {
                logger.error("operation failed times reached threshold, shutdown program");
                this.client.shutdown();
                return;
            }

            if (op.equals("batch-cancel-orders"))
                cancelAllPendingOrders();
            else
                placeAllNext();
        }
    }

    private void acquireSemaphore() {
        try {
            operating.acquire();
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
