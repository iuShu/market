package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.PacketUtils;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OperationFailedEvent;
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

import static org.iushu.market.Constants.ORDER_STATE_FILLED;
import static org.iushu.market.Constants.ORDER_TYPE_LIMIT;
import static org.iushu.market.trade.MartinOrderUtils.*;
import static org.iushu.market.trade.okx.OkxConstants.*;

@OkxComponent
public class Tracker implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(Tracker.class);

    private final TradingProperties properties;
    private final OkxRestTemplate restTemplate;
    private ApplicationEventPublisher eventPublisher;

    private volatile double firstPx = 0.0;
    private volatile String messageId = "";

    public Tracker(TradingProperties properties, OkxRestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onFirstOrderFilled(OkxWebSocketSession session, JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        int contractSize = data.getIntValue("sz", -1);
        double accFillSz = data.getDoubleValue("accFillSz");
        String state = data.getString("state");
        String side = data.getString("side");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        if (contractSize != properties.getOrder().getFirstContractSize() || accFillSz < contractSize
                || !ORDER_STATE_FILLED.equals(state) || !side.equals(posSide.openSide()))
            return;

        firstPx = data.getDoubleValue("avgPx");
        placeFollowOrders(session, posSide);
        addMarginBalance(posSide);
    }

    @EventListener(OrderSuccessorEvent.class)
    public void onOrderSuccessor(OrderSuccessorEvent event) {
        if (!Successor.FIRST_ORDER.equals(event.getType()))
            return;

        JSONObject filled = (JSONObject) event.getSource();
        this.firstPx = filled.getDoubleValue("avgPx");
    }

    private void placeFollowOrders(OkxWebSocketSession session, PosSide posSide) {
        List<JSONObject> packets = new ArrayList<>();
        for (int i = 0; i < properties.getOrder().getMaxOrder() - 1; i++) {
            int cs = contractSize(i, properties.getOrder());
            double nextOrderPrice = nextOrderPrice(firstPx, cs, posSide, properties.getOrder());
            cs = nextContractSize(cs, properties.getOrder());
            packets.add(PacketUtils.orderPacket(properties, posSide.openSide(), posSide, ORDER_TYPE_LIMIT, cs, nextOrderPrice));
        }
        JSONObject packet = PacketUtils.placeOrdersPacket(packets);
        if (session.sendPrivateMessage(packet)) {
            messageId = packet.getString("id");
            return;
        }

        String errMsg = String.format("send %d follow orders failed", packets.size());
        logger.warn(errMsg);
        eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
    }

    private void addMarginBalance(PosSide posSide) {
        double extraMargin = properties.getOrder().getExtraMargin();
        if (extraMargin <= 0)
            return;

        if (restTemplate.addExtraMargin(posSide, extraMargin))
            logger.info("add extra margin {} success", extraMargin);
        else {
            String errMsg = String.format("add extra margin %s failed", extraMargin);
            logger.warn(errMsg);
            eventPublisher.publishEvent(new OperationFailedEvent(errMsg));
        }
    }

    @SubscribeChannel(op = OP_BATCH_ORDERS)
    public void followOrderResponse(JSONObject message) {
        if (!message.getString("id").equals(messageId))
            return;

        int code = message.getIntValue("code", -1);
        JSONArray data = message.getJSONArray("data");
        if (SUCCESS == code) {
            messageId = "";
            logger.info("placed {} follow orders", data.size());
            return;
        }

        logger.error("place {} follow orders failed {}", data.size(), message);
        eventPublisher.publishEvent(new OrderErrorEvent(message));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
