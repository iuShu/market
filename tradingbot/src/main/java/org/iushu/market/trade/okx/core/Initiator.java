package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.FileSignal;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.*;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderErrorEvent;
import org.iushu.market.trade.okx.event.OrderSuccessorEvent;
import org.iushu.market.trade.okx.event.TradingStopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.iushu.market.Constants.ORDER_TYPE_MARKET;
import static org.iushu.market.trade.MartinOrderUtils.lastContractSize;
import static org.iushu.market.trade.MartinOrderUtils.totalCost;
import static org.iushu.market.trade.okx.OkxConstants.*;

@OkxComponent
public class Initiator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Initiator.class);

    private final OkxRestTemplate restTemplate;
    private final TradingProperties properties;
    private ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;
    private final Strategy<Double> strategy;
    private final FileSignal fileSignal;

    private volatile double balance = 0.0;
    private volatile String messageId = "";
    private final AtomicBoolean existed = new AtomicBoolean(false);
    private final AtomicInteger failure = new AtomicInteger(5);
    private final AtomicInteger batch = new AtomicInteger(1);

    public Initiator(Strategy<Double> strategy, OkxRestTemplate restTemplate, TradingProperties properties, TaskScheduler taskScheduler, FileSignal fileSignal) {
        this.strategy = strategy;
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.fileSignal = fileSignal;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void preCheck() {
        JSONArray leverages = restTemplate.getLeverage();
        for (int i = 0; i < leverages.size(); i++) {
            JSONObject each = leverages.getJSONObject(i);
            PosSide posSide = PosSide.of(each.getString("posSide"));
            int lever = each.getIntValue("lever", -1);
            if (lever == properties.getLever())
                continue;

            if (!restTemplate.setLeverage(posSide, properties.getLever())) {
                logger.error("set {} lever from {} to {} error", posSide.getName(), lever, properties.getLever());
                System.exit(1);
            }
            logger.info("set {} lever from {} to {}", posSide.getName(), lever, properties.getLever());
        }

        refreshAccountBalance();

        logger.info("start trading for {} {} {}x", properties.getInstId(), properties.getTdMode(), properties.getLever());
        logger.info("account balance {} {}", balance, properties.getCurrency());
    }

    @SubscribeChannel(channel = CHANNEL_TICKERS)
    public void placeFirstOrder(OkxWebSocketSession session, JSONObject message) {
        if (existed.get() || !session.isActive(session.getPrivateSession()))
            return;

        JSONArray data = message.getJSONArray("data");
        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");

        PosSide posSide = fileSignal.manualSide();
        posSide = posSide == null ? strategy.trend(price) : posSide;
        if (posSide == null)
            return;

        double cost = totalCost(price, lastContractSize(properties.getOrder()), posSide, properties.getLever(), properties.getOrder());
        if (balance < cost) {
            String errMsg = String.format("account balance not enough for cost %s", cost);
            logger.error(errMsg);
            eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
            return;
        }

        logger.debug("recommend {}", posSide.getName());
        JSONObject packet = PacketUtils.placeOrderPacket(properties, posSide.openSide(), posSide, ORDER_TYPE_MARKET,
                properties.getOrder().getFirstContractSize(), 0.0);

        if (existed.get() || !existed.compareAndSet(false, true))
            return;

        if (session.sendPrivateMessage(packet)) {
            logger.info("sent first order {} {} {}", price, properties.getOrder().getFirstContractSize(), posSide.getName());
            messageId = packet.getString("id");
        }
        else {
            String errMsg = "send first order failed";
            logger.warn(errMsg);
            existed.compareAndSet(true, false);
        }
    }

    @SubscribeChannel(op = OP_ORDER)
    public void placeResponse(JSONObject message) {
        String mid = message.getString("id");
        if (!messageId.equals(mid))
            return;

        int code = message.getIntValue("code", -1);
        if (SUCCESS == code) {
            messageId = "";
            failure.set(5);
            return;
        }

        if (failure.getAndDecrement() == 0) {
            logger.error("place first order failed too many times");
            eventPublisher.publishEvent(new OrderErrorEvent("place first order failed too many times"));
            return;
        }
        logger.error("place first order failed {}", message.toString());
        existed.compareAndSet(true, false);     // recover
    }

    @EventListener(OrderSuccessorEvent.class)
    public void onOrderSuccessor(OrderSuccessorEvent event) {
        if (!event.getType().equals(Successor.FIRST_ORDER))
            return;
        if (!existed.compareAndSet(false, true)) {
            logger.error("successor processing failed");
            eventPublisher.publishEvent(new OrderErrorEvent("Initiator process successor event error"));
        }
    }

    @EventListener(OrderClosedEvent.class)
    public void onOrderClose(OrderClosedEvent event) {
        refreshAccountBalance();
        JSONObject data = (JSONObject) event.getSource();
        String pnl = data.getString("pnl");
//        logger.info("closed by take profit at {}", data.getDoubleValue("px"));
        logger.info("{} batch order closed, balance {} {}, ready to next round", batch.getAndIncrement(), balance, pnl);
        if (!fileSignal.isStop()) {
            taskScheduler.schedule(() -> existed.compareAndSet(true, false), new Date(System.currentTimeMillis() + 5000));
            return;
        }

        logger.info("found stop signal, system exit");
        eventPublisher.publishEvent(TradingStopEvent.event());
    }

    private void refreshAccountBalance() {
        double balance = restTemplate.getBalance();
        if (balance <= 0)
            throw new IllegalStateException("deficient balance for trading");
        this.balance = balance;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
