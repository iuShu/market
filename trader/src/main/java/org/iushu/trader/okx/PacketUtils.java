package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.okx.martin.Order;

import java.util.Collection;
import java.util.Random;

public class PacketUtils {

    private static final Random random = new Random();

    private static long identifier() {
        return System.currentTimeMillis() * 1000 + random.nextInt(1000);
    }

    public static JSONObject placeOrderPacket(Order order) {
        JSONArray args = JSONArray.of(orderPacket(order));
        JSONObject packet = JSONObject.of("op", "order");
        packet.put("id", Long.toString(identifier()));
        packet.put("args", args);
        return packet;
    }

    public static JSONObject placeOrdersPacket(Collection<Order> orderList) {
        JSONArray args = new JSONArray();
        orderList.forEach(order -> args.add(orderPacket(order)));
        JSONObject packet = JSONObject.of("op", "batch-orders");
        packet.put("id", Long.toString(identifier()));
        packet.put("args", args);
        return packet;
    }

    public static JSONObject orderPacket(Order order) {
        JSONObject data = JSONObject.of("instId", Setting.INST_ID);
        data.put("tdMode", Setting.TD_MODE);
        data.put("side", order.getSide());
        data.put("posSide", order.getPosSide().getName());
        data.put("ordType", order.getOrderType());
        data.put("sz", Integer.toString(order.getPosition()));
        if (order.getPrice() > 0)
            data.put("px", Double.toString(order.getPrice()));
        return data;
    }

}
