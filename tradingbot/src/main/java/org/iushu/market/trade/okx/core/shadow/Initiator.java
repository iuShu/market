package org.iushu.market.trade.okx.core.shadow;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.Constants;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.Strategy;
import org.iushu.market.trade.okx.config.OkxShadowComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderErrorEvent;
import org.iushu.market.trade.okx.event.OrderFilledEvent;
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

import static org.iushu.market.trade.MartinOrderUtils.lastContractSize;
import static org.iushu.market.trade.MartinOrderUtils.totalCost;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_TICKERS;

@OkxShadowComponent("shadowInitiator")
public class Initiator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Initiator.class);

    private final OkxRestTemplate restTemplate;
    private final TradingProperties properties;
    private ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;

    private final Strategy<Double> strategy;
    private volatile double balance = 0.0;
    private final AtomicBoolean existed = new AtomicBoolean(false);
    private final AtomicInteger batch = new AtomicInteger(1);

    public Initiator(Strategy<Double> strategy, OkxRestTemplate restTemplate, TradingProperties properties, TaskScheduler taskScheduler) {
        this.strategy = strategy;
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
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
        if (existed.get())
            return;

        JSONArray data = message.getJSONArray("data");
        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");
        PosSide posSide = strategy.trend(price);
        if (posSide == null)
            return;

        double cost = totalCost(price, lastContractSize(properties.getOrder()), posSide, properties.getLever(), properties.getOrder());
        logger.info("{} cost {} at {}", batch.get(), cost, price);

        if (existed.get() || !existed.compareAndSet(false, true))
            return;

        logger.info("sent first order {} {}", price, properties.getOrder().getFirstContractSize());

        JSONObject filled = JSONObject.of("sz", properties.getOrder().getFirstContractSize());
        filled.put("avgPx", price);
        filled.put("posSide", posSide.getName());
        filled.put("accFillSz", properties.getOrder().getFirstContractSize());
        filled.put("state", Constants.ORDER_STATE_FILLED);
        filled.put("side", posSide.openSide());
        eventPublisher.publishEvent(new OrderFilledEvent(filled));
    }

    @EventListener(OrderClosedEvent.class)
    public void onOrderClose(OrderClosedEvent event) {
        taskScheduler.schedule(() -> existed.compareAndSet(true, false), new Date(System.currentTimeMillis() + 5000));
        refreshAccountBalance();
        logger.info("{} batch order closed, balance {}, ready to next round", batch.getAndIncrement(), balance);
    }

    private void refreshAccountBalance() {
        this.balance = restTemplate.getBalance();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
