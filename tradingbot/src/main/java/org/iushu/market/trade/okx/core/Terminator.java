package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.DispatchManager;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.PacketUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.iushu.market.Constants.ORDER_STATE_FILLED;
import static org.iushu.market.Constants.ORDER_STATE_LIVE;
import static org.iushu.market.trade.MartinOrderUtils.takeProfitPrice;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_ORDERS;
import static org.iushu.market.trade.okx.OkxConstants.CHANNEL_TICKERS;

@OkxComponent
public class Terminator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Terminator.class);

    private final TradingProperties properties;
    private final OkxRestTemplate restTemplate;
    private ApplicationEventPublisher eventPublisher;

    private final Map<String, String> orderStateMap;
    private volatile int orderPos;
    private volatile PosSide posSide;
    private final AtomicReference<Double> firstPx = new AtomicReference<>(0.0);

    public Terminator(TradingProperties properties, OkxRestTemplate restTemplate) {
        this.properties = properties;
        this.orderStateMap = new ConcurrentHashMap<>(properties.getOrder().getMaxOrder());
        this.restTemplate = restTemplate;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onOrderClosed(OkxWebSocketSession session, JSONObject message, DispatchManager manager) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        String ordId = data.getString("ordId");
        String state = data.getString("state");
        String side = data.getString("side");
        orderStateMap.put(ordId, state);
        if (!ORDER_STATE_FILLED.equals(state) || !posSide.closeSide().equals(side))
            return;

        if (!cancelLiveOrders(session)) {
            String errMsg = "cancel live orders error";
            logger.error(errMsg);
            eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
            return;
        }
        orderStateMap.clear();
        firstPx.set(0.0);
        eventPublisher.publishEvent(new OrderClosedEvent(null));
    }

    // can be merged to onOrderClosed(..)
    @EventListener(OrderFilledEvent.class)
    public void recordOrderFilled(OrderFilledEvent event) {
        JSONObject message = (JSONObject) event.getSource();
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        orderPos = data.getIntValue("pos", -1);
        if (orderPos == properties.getOrder().getPosStart()) {
            posSide = PosSide.of(data.getString("posSide"));
            firstPx.set(data.getDoubleValue("fillPx"));
        }
    }

    @SubscribeChannel(channel = CHANNEL_TICKERS)
    public void takeProfitCheck(OkxWebSocketSession session, JSONObject message) {
        if (posSide == null)
            return;

        JSONArray data = message.getJSONArray("data");
        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");
        double tpPx = takeProfitPrice(firstPx.get(), orderPos, posSide, properties.getOrder());
        if (!posSide.isProfit(tpPx, price))
            return;

        if (!restTemplate.closePosition(posSide)) {
            String errMsg = String.format("close pos error by rest api at %s", price);
            logger.error(errMsg);
            eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
        }
        else {
            logger.info("close pos by take profit at {} {}", price, orderPos);
        }
    }

    private boolean cancelLiveOrders(OkxWebSocketSession session) {
        List<String> lives = new ArrayList<>();
        orderStateMap.forEach((k, v) -> {
            if (v.equals(ORDER_STATE_LIVE))
                lives.add(k);
        });
        if (lives.isEmpty())
            return true;

        logger.info("send cancel {} live orders", lives.size());
        JSONObject packet = PacketUtils.cancelOrdersPacket(lives, properties.getInstId());
        return session.sendMessage(packet);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
