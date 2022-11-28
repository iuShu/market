package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxWebSocketSession;
import org.iushu.market.trade.okx.PacketUtils;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.iushu.market.trade.okx.event.OrderErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;

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
    private ApplicationEventPublisher eventPublisher;

    private volatile double firstPx = 0.0;
    private volatile String messageId = "";

    public Tracker(TradingProperties properties) {
        this.properties = properties;
    }

    @SubscribeChannel(channel = CHANNEL_ORDERS)
    public void onFirstOrderFilled(OkxWebSocketSession session, JSONObject message) {
        JSONObject data = message.getJSONArray("data").getJSONObject(0);
        int pos = data.getIntValue("sz", -1);
        String state = data.getString("state");
        String side = data.getString("side");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        if (pos != properties.getOrder().getPosStart() || !ORDER_STATE_FILLED.equals(state) || !side.equals(posSide.openSide()))
            return;

        firstPx = data.getDoubleValue("fillPx");
        placeFollowOrders(session, posSide);
        addMarginBalance();
    }

    private void placeFollowOrders(OkxWebSocketSession session, PosSide posSide) {
        List<JSONObject> packets = new ArrayList<>();
        for (int i = 0; i < properties.getOrder().getMaxOrder() - 1; i++) {
            int pos = orderPos(i, properties.getOrder());
            double nextOrderPrice = nextOrderPrice(firstPx, pos, posSide, properties.getOrder());
            pos = nextPosition(pos, properties.getOrder());
            packets.add(PacketUtils.orderPacket(properties, posSide.openSide(), posSide, ORDER_TYPE_LIMIT, pos, nextOrderPrice));
        }
        JSONObject packet = PacketUtils.placeOrdersPacket(packets);
        if (session.sendMessage(packet)) {
            messageId = packet.getString("id");
            return;
        }

        String errMsg = String.format("send %d follow orders failed", packets.size());
        logger.warn(errMsg);
        eventPublisher.publishEvent(new OrderErrorEvent(errMsg));
    }

    private void addMarginBalance() {



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

        String errMsg = String.format("place %d follow orders failed", data.size());
        logger.error(errMsg);
        eventPublisher.publishEvent(new OrderErrorEvent(message));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.eventPublisher = applicationContext;
    }
}
