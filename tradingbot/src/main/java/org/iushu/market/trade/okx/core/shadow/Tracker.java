package org.iushu.market.trade.okx.core.shadow;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.Constants;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.config.OkxShadowComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderFilledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.iushu.market.Constants.ORDER_STATE_FILLED;
import static org.iushu.market.trade.MartinOrderUtils.*;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_TICKERS;

@OkxShadowComponent("shadowTracker")
public class Tracker implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Tracker.class);

    private final TradingProperties properties;
    private ApplicationEventPublisher eventPublisher;

    private volatile double firstPx = 0.0;
    private volatile PosSide posSide = null;
    private final AtomicInteger idx = new AtomicInteger(0);
    private final AtomicBoolean processing = new AtomicBoolean(false);

    public Tracker(TradingProperties properties) {
        this.properties = properties;
    }

    @SubscribeChannel(channel = CHANNEL_TICKERS)
    public void fillOrCloseOrder(JSONObject message) {
        if (posSide == null || firstPx == 0)
            return;

        JSONArray data = message.getJSONArray("data");
        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");
        int cs = contractSize(idx.get(), properties.getOrder());
        double nextOrderPrice = nextOrderPrice(firstPx, cs, posSide, properties.getOrder());
        if (!posSide.isLoss(nextOrderPrice, price) || !processing.compareAndSet(false, true))
            return;

        if (nextOrderPrice == stopLossPrice(firstPx, posSide, properties.getOrder())) {
            firstPx = 0.0;
            posSide = null;
            idx.set(0);
            eventPublisher.publishEvent(new OrderClosedEvent("order failed at " + price));
            logger.warn("order failed this round at {}", price);
        }
        else {
            idx.incrementAndGet();
            cs = nextContractSize(cs, properties.getOrder());
            JSONObject filled = JSONObject.of("sz", cs);
            filled.put("avgPx", nextOrderPrice);
            filled.put("posSide", posSide);
            filled.put("accFillSz", cs);
            filled.put("state", Constants.ORDER_STATE_FILLED);
            filled.put("side", posSide.openSide());
            eventPublisher.publishEvent(new OrderFilledEvent(filled));
        }
        processing.compareAndSet(true, false);
    }

    @EventListener(OrderFilledEvent.class)
    public void firstOrderFilled(OrderFilledEvent event) {
        JSONObject data = (JSONObject) event.getSource();
        int contractSize = data.getIntValue("sz", -1);
        double accFillSz = data.getDoubleValue("accFillSz");
        String state = data.getString("state");
        String side = data.getString("side");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        if (contractSize != properties.getOrder().getFirstContractSize() || accFillSz < contractSize
                || !ORDER_STATE_FILLED.equals(state) || !side.equals(posSide.openSide()))
            return;

        this.firstPx = data.getDoubleValue("avgPx");
        this.posSide = posSide;
        placeFollowOrders(posSide);
        addMarginBalance();
    }

    private void placeFollowOrders(PosSide posSide) {
        for (int i = 0; i < properties.getOrder().getMaxOrder() - 1; i++) {
            int cs = contractSize(i, properties.getOrder());
            double nextOrderPrice = nextOrderPrice(firstPx, cs, posSide, properties.getOrder());
            cs = nextContractSize(cs, properties.getOrder());
            logger.info("placed follow order {} {} {}", cs, posSide.getName(), nextOrderPrice);
        }
    }

    private void addMarginBalance() {
        logger.info("add extra margin {} success", properties.getOrder().getExtraMargin());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
