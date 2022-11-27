package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.*;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import static org.iushu.market.trade.okx.OkxConstants.*;

@OkxComponent
public class Initiator {

    private static final Logger logger = LoggerFactory.getLogger(Initiator.class);

    private final Strategy<Double> strategy;
    private final OkxRestTemplate restTemplate;
    private final TradingProperties properties;
    private volatile double balance = 0.0;
    private volatile String messageId = "";
    private volatile boolean existed = false;

    public Initiator(Strategy<Double> strategy, OkxRestTemplate restTemplate, TradingProperties properties) {
        this.strategy = strategy;
        this.restTemplate = restTemplate;
        this.properties = properties;
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
        if (existed)
            return;

        JSONArray data = message.getJSONArray("data");
        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");
        PosSide posSide = strategy.trend(price);
        if (posSide == null)
            return;

        // send order-placing packet
        logger.debug("recommend {}", posSide.getName());
    }

    @SubscribeChannel(op = OP_ORDER)
    public void placeResponse(JSONObject message) {
        String mid = message.getString("id");
        if (!messageId.equals(mid))
            return;

        int code = message.getIntValue("code", -1);
        if (SUCCESS != code)
            return;

        existed = true;
        messageId = "";
        logger.info("placed first order");
    }

    private void refreshAccountBalance() {
        double balance = restTemplate.getBalance();
        if (balance <= 0)
            throw new IllegalStateException("deficient balance for trading");
        this.balance = balance;
    }

}
