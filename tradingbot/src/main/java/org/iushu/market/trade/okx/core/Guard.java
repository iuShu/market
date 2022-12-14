package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OperationFailedEvent;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderFilledEvent;
import org.iushu.market.trade.okx.event.OrderSuccessorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private volatile int algoSz = 0;
    private final Lock lock = new ReentrantLock();

    public Guard(TradingProperties properties, OkxRestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onOrderFilled(JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        int contractSize = data.getIntValue("sz", -1);
        double accFillSz = data.getDoubleValue("accFillSz");
        String side = data.getString("side");
        String state = data.getString("state");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        if (!state.equals(ORDER_STATE_FILLED) || !side.equals(posSide.openSide()))
            return;

        logger.info("filled data {}", data);    // dev
        if (accFillSz < contractSize)           // filter partial filled
            return;

        double px = data.getDoubleValue("avgPx");
        if (contractSize == properties.getOrder().getFirstContractSize())
            firstPx = px;

        data.put("_ttlCs", totalContractSize(contractSize, properties.getOrder()));
        data.put("_tpPx", takeProfitPrice(firstPx, contractSize, posSide, properties.getOrder()));
        data.put("_slPx", stopLossPrice(firstPx, posSide, properties.getOrder()));
        eventPublisher.publishEvent(new OrderFilledEvent(data));

        lock.lock();
        try {
            placeAlgoOrder(data, contractSize, posSide);
        } finally {
            lock.unlock();
        }
    }

    private void placeAlgoOrder(JSONObject data, double px, PosSide posSide) {
        int ttlCs = data.getIntValue("_ttlCs", -1);
        if (algoSz > ttlCs || (algoId.length() > 0 && !cancelPreviousAlgo()))
            return;

        double tpPx = data.getDoubleValue("_tpPx");
        double slPx = data.getDoubleValue("_slPx");
        JSONObject response = restTemplate.placeAlgoOrder(posSide, posSide.closeSide(), ttlCs, tpPx, slPx);
        if (response.isEmpty())     // retry
            response = restTemplate.placeAlgoOrder(posSide, posSide.closeSide(), ttlCs, tpPx, slPx);
        if (!response.isEmpty()) {
            algoId = response.getString("algoId");
            algoSz = ttlCs;
            logger.info("placed algo {} tp={} sl={} {}", ttlCs, tpPx, slPx, algoId);
            return;
        }

        String errMsg = String.format("place algo failed for %s", px);
        logger.warn(errMsg);
        eventPublisher.publishEvent(new OperationFailedEvent(errMsg));
    }

    private boolean cancelPreviousAlgo() {
        if (restTemplate.cancelAlgoOrder(algoId)) {
            logger.info("canceled previous algo {}", algoId);
            algoId = "";
            return true;
        }

        String errMsg = String.format("cancel previous algo failed %s", algoId);
        logger.warn(errMsg);
        eventPublisher.publishEvent(new OperationFailedEvent(errMsg));
        return false;
    }

    @EventListener(OrderSuccessorEvent.class)
    public void onOrderSuccessor(OrderSuccessorEvent event) {
        if (Successor.ALGO_ORDER.equals(event.getType())) {
            JSONObject algo = (JSONObject) event.getSource();
            algoId = algo.getString("algoId");
            algoSz = algo.getIntValue("sz");
        }
        else if (Successor.FIRST_ORDER.equals(event.getType())) {
            JSONObject filled = (JSONObject) event.getSource();
            firstPx = filled.getDoubleValue("avgPx");
        }
    }

    @EventListener(OrderClosedEvent.class)
    public void onOrderClosed() {
        algoId = "";
        algoSz = 0;
        firstPx = 0.0;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
