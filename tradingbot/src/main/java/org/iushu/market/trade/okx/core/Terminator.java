package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.component.ProfileContext;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.PacketUtils;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderErrorEvent;
import org.iushu.market.trade.okx.event.OrderSuccessorEvent;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.iushu.market.Constants.ORDER_STATE_FILLED;
import static org.iushu.market.Constants.ORDER_STATE_LIVE;
import static org.iushu.market.component.ProfileContext.PROD;
import static org.iushu.market.component.ProfileContext.TEST;
import static org.iushu.market.trade.MartinOrderUtils.takeProfitPrice;
import static org.iushu.market.trade.okx.OkxConstants.*;

@OkxComponent
public class Terminator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Terminator.class);

    private final TradingProperties properties;
    private final OkxRestTemplate restTemplate;
    private ApplicationEventPublisher eventPublisher;

    private final Map<String, String> orderStateMap;
    private volatile int orderContractSize;
    private volatile PosSide posSide;
    private volatile String messageId = "";
    private final AtomicReference<Double> firstPx = new AtomicReference<>(0.0);
    private final List<Double> prices = new CopyOnWriteArrayList<>();

    public Terminator(TradingProperties properties, OkxRestTemplate restTemplate) {
        this.properties = properties;
        this.orderStateMap = new ConcurrentHashMap<>(properties.getOrder().getMaxOrder());
        this.restTemplate = restTemplate;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onOrderClosed(OkxWebSocketSession session, JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        String ordId = data.getString("ordId");
        String state = data.getString("state");
        String side = data.getString("side");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        int contractSize = data.getIntValue("sz", -1);
        orderStateMap.put(ordId, state);
        orderContractSize = contractSize;
        if (!ORDER_STATE_FILLED.equals(state) || !posSide.closeSide().equals(side))
            return;

        if (contractSize == properties.getOrder().getFirstContractSize()) {
            this.posSide = posSide;
            this.firstPx.set(data.getDoubleValue("avgPx"));
        }

        cancelLiveOrders(session);
        firstPx.set(0.0);
        this.posSide = null;
        orderStateMap.clear();
        eventPublisher.publishEvent(new OrderClosedEvent(data));
    }

    @EventListener(OrderSuccessorEvent.class)
    public void onOrderSuccessor(OrderSuccessorEvent event) {
        if (Successor.FIRST_ORDER.equals(event.getType())) {
            JSONObject filled = (JSONObject) event.getSource();
            posSide = PosSide.of(filled.getString("posSide"));
            firstPx.set(filled.getDoubleValue("avgPx"));
            orderContractSize = filled.getIntValue("lastFillSz", -1);
        }
        else if (Successor.PENDING_ORDER.equals(event.getType())) {
            JSONArray orders = (JSONArray) event.getSource();
            for (int i = 0; i < orders.size(); i++) {
                String ordId = orders.getJSONObject(i).getString("ordId");
                orderStateMap.put(ordId, ORDER_STATE_LIVE);
            }
        }
    }

    @SubscribeChannel(channel = CHANNEL_TICKERS)
    public void takeProfitCheck(JSONObject message) {
        if (posSide == null || firstPx.get() == 0)
            return;

        JSONArray data = message.getJSONArray("data");
        JSONObject ticker = data.getJSONObject(0);
        double price = ticker.getDoubleValue("last");
        double tpPx = takeProfitPrice(firstPx.get(), orderContractSize, posSide, properties.getOrder());
        if (!canTakeProfit(price, tpPx))
            return;

        if (!restTemplate.closePosition(posSide)) {
            String errMsg = String.format("close pos error %s %s", price, posSide.getName());
            logger.error(errMsg);
//            eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
        }
        else {
            logger.info("close pos by take profit at {} {}", price, orderContractSize);
        }
    }

    private boolean canTakeProfit(double tpPx, double px) {
        if (ProfileContext.isProfile(PROD))
            return posSide.isProfit(tpPx, px);

        if (prices.size() >= 5)
            prices.remove(0);
        prices.add(px);

        for (Double price : prices) {
            if (!posSide.isProfit(tpPx, price))
                return false;
        }
        return true;
    }

    private void cancelLiveOrders(OkxWebSocketSession session) {
        List<String> lives = new ArrayList<>();
        orderStateMap.forEach((k, v) -> {
            if (v.equals(ORDER_STATE_LIVE))
                lives.add(k);
        });
        if (lives.isEmpty())
            return;

        JSONObject packet = PacketUtils.cancelOrdersPacket(lives, properties.getInstId());
        messageId = packet.getString("id");
        if (session.sendPrivateMessage(packet))
            return;

        String errMsg = "send cancel live orders error";
        logger.error(errMsg);
        eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
    }

    @SubscribeChannel(op = EVENT_BATCH_CANCEL_ORDERS)
    public void cancelOrderResponse(JSONObject message) {
        if (!message.getString("id").equals(messageId))
            return;

        int code = message.getIntValue("code", -1);
        JSONArray data = message.getJSONArray("data");
        if (SUCCESS == code) {
            messageId = "";
            logger.info("canceled {} live orders", data.size());
            return;
        }

        logger.error("cancel {} live orders failed {}", data.size(), message);
        eventPublisher.publishEvent(new OrderErrorEvent(message));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
