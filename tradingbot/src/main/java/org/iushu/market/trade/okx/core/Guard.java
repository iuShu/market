package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.config.OkxComponent;
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
import org.springframework.context.event.EventListener;

import static org.iushu.market.Constants.ORDER_STATE_FILLED;
import static org.iushu.market.trade.MartinOrderUtils.*;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_ORDERS;

@OkxComponent
public class Guard implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Guard.class);

    private final TradingProperties properties;
    private final OkxRestTemplate restTemplate;
    private ApplicationEventPublisher eventPublisher;

    private volatile double firstPx = 0.0;
    private volatile String algoId = "";

    public Guard(TradingProperties properties, OkxRestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onOrderFilled(JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        int pos = data.getIntValue("sz", -1);
        String side = data.getString("side");
        String state = data.getString("state");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        if (!state.equals(ORDER_STATE_FILLED) || !side.equals(posSide.openSide()))
            return;

        double px = data.getDoubleValue("fillPx");
        if (pos == properties.getOrder().getPosStart())
            firstPx = px;

        if (data.getDoubleValue("fillSz") < pos)    // partial filled
            return;

        data.put("_ttlPos", totalPosition(pos, properties.getOrder()));
        data.put("_tpPx", takeProfitPrice(firstPx, pos, posSide, properties.getOrder()));
        data.put("_slPx", stopLossPrice(firstPx, posSide, properties.getOrder()));
        eventPublisher.publishEvent(new OrderFilledEvent(data));
        placeAlgoOrder(data, pos, posSide);
    }

    private void placeAlgoOrder(JSONObject data, double px, PosSide posSide) {
        if (algoId.length() > 0 && !cancelPreviousAlgo())
            return;

        int totalPos = data.getIntValue("_ttlPos", -1);
        double tpPx = data.getDoubleValue("_tpPx");
        double slPx = data.getDoubleValue("_slPx");
        JSONObject response = restTemplate.placeAlgoOrder(posSide, posSide.closeSide(), totalPos, tpPx, slPx);
        if (!response.isEmpty()) {
            algoId = response.getString("algoId");
            logger.info("placed algo {} tp={} sl={}", totalPos, tpPx, slPx);
            return;
        }

        String errMsg = String.format("place algo failed for %s", px);
        logger.error(errMsg);
        eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
    }

    private boolean cancelPreviousAlgo() {
        if (restTemplate.cancelAlgoOrder(algoId)) {
            logger.info("canceled previous algo {}", algoId);
            return true;
        }

        String errMsg = String.format("cancel previous algo failed %s", algoId);
        logger.error(errMsg);
        eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
        return false;
    }

    @EventListener(OrderClosedEvent.class)
    public void onOrderClosed() {
        algoId = "";
        firstPx = 0.0;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}