package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.base.Signature;
import org.iushu.trader.okx.martin.MartinOrders;
import org.iushu.trader.okx.martin.Order;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class PacketUtils {

    private static final Random random = new Random();

    private static long identifier() {
        return System.currentTimeMillis() * 1000 + random.nextInt(1000);
    }

    public static JSONObject loginPacket() {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = Signature.sign(timestamp + "GET/users/self/verify", Setting.SECRETKEY);
        JSONObject login = JSONObject.of("apiKey", Setting.APIKEY);
        login.put("passphrase", Setting.PASSPHRASE);
        login.put("timestamp", timestamp);
        login.put("sign", signature);
        JSONArray args = JSONArray.of(login);
        return JSONObject.of("op", "login", "args", args);
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

    public static JSONObject cancelOrdersPacket() {
        Collection<Order> orders = MartinOrders.instance().allOrders();
        List<Order> lives = orders.stream().filter(o -> Constants.ORDER_STATE_LIVE.equals(o.getState())).collect(Collectors.toList());
        JSONObject packet = JSONObject.of("id", Long.toString(identifier()), "op", "batch-cancel-orders");
        JSONArray args = new JSONArray();
        lives.forEach(live -> args.add(JSONObject.of("instId", Setting.INST_ID, "ordId", live.getOrderId())));
        packet.put("args", args);
        return packet;
    }

}
