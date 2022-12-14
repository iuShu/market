package org.iushu.trader.okx.martin.version2;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.Trader;
import org.iushu.trader.base.NotifyRobot;
import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.OkxHttpUtils;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.PacketUtils;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.okx.martin.Order;
import org.iushu.trader.websocket.WsJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.iushu.trader.base.Constants.*;

public class Tracker implements OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(Tracker.class);

    private WsJsonClient client;
    private final Semaphore operating = new Semaphore(1);
    private volatile String messageId = "";
    private final AtomicInteger failedTimes = new AtomicInteger(0);

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
            case ORDER_STATE_LIVE:
                Order plain = martinOrders.getOrder(position);
                if (plain == null || plain.getOrderId() != null)
                    return;     // placed close all position or order has been filled partially

                plain.setOrderId(ordId);
                plain.setCreateTime(order.getLong("cTime"));
                plain.setUpdateTime(update);
                logger.info("placed order {} pos={} {}", ordId, position, order.getString("px"));
                break;
            case ORDER_STATE_FILLED:
                if (martinOrders.closeSide().equals(side)) {
                    logger.debug("received close order");
                    cancelAllPendingOrders();
                    double pnl = message.getDoubleValue("pnl");
                    if (pnl < 0) {
                        String fatalMsg = String.format("[%s] orders lost %s", MartinOrders.instance().getBatch(), pnl);
                        logger.error(fatalMsg);
                        NotifyRobot.sendFatal(fatalMsg);
                        Trader.instance().stop();
                    }
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
                    addExtraMargin();
                }

                placeAlgoOrder();
                if (martinOrders.isLastOrder(live))
                    logger.warn("last order has been filled at {} {}", live.getPrice(), position);
                this.notifyOrderFilled();
                break;
            case ORDER_STATE_CANCELED:
                logger.info("canceled order {} pos={}", ordId, position);
//                Order filled = martinOrders.getOrder(position);
//                if (filled == null || martinOrders.openSide().equals(side)) {
//                    logger.warn("canceled order not found, {} ", message.toJSONString());
//                    return;
//                }
//                filled.setState(state);
//                filled.setUpdateTime(update);
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
                if (r.isOK())
                    logger.info("sent {} next orders", orders.size());
                else {
                    String fatalMsg = String.format("send %s next orders failed", orders.size());
                    logger.error(fatalMsg);
                    NotifyRobot.sendFatal(fatalMsg);
                    Trader.instance().stop();
                }
                operating.release();
            });
        } catch (Exception e) {
            logger.error("place all next error", e);
            NotifyRobot.sendFatal("place all next order error");
            Trader.instance().stop();
            operating.release();
        }
    }

    private void addExtraMargin() {
        PosSide posSide = MartinOrders.instance().first().getPosSide();
        double margin = MartinOrders.instance().totalExtraMargin();
        for (int i = 0; i < Setting.OPERATION_MAX_FAILURE_TIMES; i++) {
            if (OkxHttpUtils.addExtraMargin(posSide, margin)) {
                logger.info("add margin {} ok", margin);
                return;
            }
        }
        String errorMsg = String.format("add margin error by %s %s", posSide.getName(), margin);
        logger.error(errorMsg);
        NotifyRobot.sendFatal(errorMsg);
        Trader.instance().stop();
    }

    private void placeAlgoOrder() {
        MartinOrders context = MartinOrders.instance();
        String prevAlgoId = context.getAlgoId();
        if (prevAlgoId != null) {
            if (!OkxHttpUtils.cancelAlgoOrder(prevAlgoId)) {
                if (this.failedTimes.incrementAndGet() < Setting.OPERATION_MAX_FAILURE_TIMES) {
                    logger.error("cancel prev algo failed {}", this.failedTimes.get());
                    this.placeAlgoOrder();
                    return;
                }
                logger.error("cancel prev algo failed times reached threshold, shutdown program");
                NotifyRobot.sendFatal("cancel prev algo order failed");
                Trader.instance().stop();
                return;
            }
            this.failedTimes.set(0);
            context.setAlgoId(null);
            logger.info("cancel prev algo {}", prevAlgoId);
        }

        Order current = context.current();
        int totalPosition = context.totalPosition(current);
        double tpPrice = context.takeProfitPrice(current);
        double slPrice = context.stopLossPrice();
        JSONObject resp = OkxHttpUtils.placeAlgoOrder(current.getPosSide(), context.closeSide(),
                totalPosition, tpPrice, slPrice);
        if (resp.isEmpty()) {
            if (this.failedTimes.incrementAndGet() < Setting.OPERATION_MAX_FAILURE_TIMES) {
                logger.error("place algo failed {}", this.failedTimes.get());
                this.placeAlgoOrder();
                return;
            }
            logger.error("place algo failed times reached threshold, shutdown program");
            NotifyRobot.sendFatal("place algo order failed");
            Trader.instance().stop();
        }
        else {
            this.failedTimes.set(0);
            context.setAlgoId(resp.getString("algoId"));
            logger.info("place algo of {} tp={} sl={} ok", totalPosition, tpPrice, slPrice);
        }
    }

    private void cancelAllPendingOrders() {
        acquireSemaphore();
        try {
            Collection<Order> orders = MartinOrders.instance().allOrders();
            List<Order> lives = orders.stream().filter(o -> ORDER_STATE_LIVE.equals(o.getState())).collect(Collectors.toList());
            if (lives.isEmpty()) {
                logger.warn("[{}] no orders to cancel", MartinOrders.instance().getBatch());
                resetBatch();
                return;
            }

            JSONObject packet = PacketUtils.cancelOrdersPacket(lives);
            this.messageId = packet.getString("id");
            this.client.sendAsync(packet, r -> {
                if (r.isOK())
                    logger.info("sent cancel orders");
                else {
                    logger.error("send cancel orders failed", r.getException());
                    NotifyRobot.sendText("send cancel orders failed by " + r.getException().getMessage());
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
        if (!this.messageId.equals(id))
            return;
        if (!"batch-cancel-orders".equals(op) && !"batch-orders".equals(op))
            return;

        Integer code = message.getInteger("code");
        if (code != null && code == 0) {
            this.failedTimes.set(0);
            logger.info("{} operation success", op);
            if (op.equals("batch-cancel-orders"))
                resetBatch();
        }
        else {
            logger.error("{} operation failed by {}", op, message);
            if (this.failedTimes.getAndIncrement() >= Setting.OPERATION_MAX_FAILURE_TIMES) {
                String fatalMsg = String.format("%s failed times reached threshold, shutdown program", op);
                logger.error(fatalMsg);
                NotifyRobot.sendFatal(fatalMsg);
                Trader.instance().stop();
                return;
            }

            if (op.equals("batch-cancel-orders"))
                cancelAllPendingOrders();
            else
                placeAllNext();
        }
    }

    private void resetBatch() {
        MartinOrders.instance().reset();
        logger.info("reset order to batch {}", MartinOrders.instance().getBatch());
        NotifyRobot.sendMarkdown("Order Reset", "order batch " + MartinOrders.instance().getBatch() + " closed");
    }

    private void notifyOrderFilled() {
        StringBuilder content = new StringBuilder("#### **Order Filled**\n----\n");
        MartinOrders.instance().allOrders().forEach(o -> {
            String desc = o.getState() + "\n";
            if (ORDER_STATE_FILLED.equals(o.getState()))
                desc = "**F** " + NotifyRobot.periodDesc(o.getUpdateTime()) + "\n";
            content.append(String.format("> - %d %s %s", o.getPosition(), o.getPrice(), desc));
        });
        Order current = MartinOrders.instance().current();
        double tpPx = MartinOrders.instance().takeProfitPrice(current);
        double slPx = MartinOrders.instance().stopLossPrice();
        content.append(String.format("\n*tp=%s sl=%s*", tpPx, slPx));
        NotifyRobot.sendMarkdown("Order Filled", content.toString());
    }

    private void acquireSemaphore() {
        try {
            operating.acquire();
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
