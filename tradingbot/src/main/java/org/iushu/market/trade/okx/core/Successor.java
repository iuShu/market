package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.Constants;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.event.OrderErrorEvent;
import org.iushu.market.trade.okx.event.OrderSuccessorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@OkxComponent
public class Successor implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Successor.class);

    public static final String FIRST_ORDER = "first";
    public static final String PENDING_ORDER = "pending";
    public static final String ALGO_ORDER = "algo";

    private final OkxRestTemplate restTemplate;
    private final TradingProperties properties;
    private ApplicationEventPublisher eventPublisher;

    public Successor(OkxRestTemplate restTemplate, TradingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void queryNotFinishOrder() {
        JSONArray orders = restTemplate.getOrderHistory();
        if (orders.isEmpty()) {
            logger.info("successor not found");
            return;
        }

        for (int i = 0; i < orders.size(); i++) {
            JSONObject filled = orders.getJSONObject(i);
            double pnl = filled.getDoubleValue("pnl");
            if (pnl == 0)
                continue;
            if (i == 0)
                return;

            filled = orders.getJSONObject(i - 1);
            int contractSize = filled.getIntValue("sz", -1);
            if (contractSize != properties.getOrder().getFirstContractSize()) {
                String errMsg = "query first filled order but found " + contractSize;
                logger.error(errMsg);
                eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
                return;
            }

            logger.info("successor found order {} {} {}", contractSize, filled.getString("avgPx"), filled.getString("posSide"));
            eventPublisher.publishEvent(new OrderSuccessorEvent(FIRST_ORDER, filled));
            queryPendingOrders();
            queryPendingAlgo();
            return;
        }
    }

    private void queryPendingOrders() {
        JSONArray pending = restTemplate.getPendingOrders(Constants.ORDER_TYPE_LIMIT);
        if (pending.isEmpty())
            return;

        for (int i = 0; i < pending.size(); i++) {
            JSONObject live = pending.getJSONObject(i);
            if (Constants.ORDER_STATE_LIVE.equals(live.getString("state"))
                    && live.getIntValue("sz", -1) != properties.getOrder().getFirstContractSize())
                continue;

            String errMsg = "illegal pending order " + pending.toString();
            logger.error(errMsg);
            eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
            return;
        }

        logger.info("successor found {} pending orders", pending.size());
        eventPublisher.publishEvent(new OrderSuccessorEvent(PENDING_ORDER, pending));
    }

    private void queryPendingAlgo() {
        JSONArray algo = restTemplate.getPendingAlgo();
        if (algo.isEmpty())
            return;

        if (algo.size() != 1) {
            String errMsg = "expected 1 algo order but found " + algo.size();
            logger.error(errMsg);
            eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
            return;
        }

        JSONObject pending = algo.getJSONObject(0);
        logger.info("successor found algo {} tp={} {} sl={} {}", pending.getIntValue("sz"),
                pending.getDoubleValue("tpTriggerPx"), pending.getString("tpTriggerPxType"),
                pending.getDoubleValue("slTriggerPx"), pending.getString("slTriggerPxType"));
        eventPublisher.publishEvent(new OrderSuccessorEvent(ALGO_ORDER, pending));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }

}
