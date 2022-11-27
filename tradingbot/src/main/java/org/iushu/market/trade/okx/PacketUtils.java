package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.component.Signature;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;

import java.util.List;
import java.util.Random;

public class PacketUtils {

    private static final Random random = new Random();

    private static long identifier() {
        return System.currentTimeMillis() * 1000 + random.nextInt(1000);
    }

    public static JSONObject loginPacket(String apiKey, String secret, String passphrase) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = Signature.sign(timestamp + "GET/users/self/verify", secret);
        JSONObject login = JSONObject.of("apiKey", apiKey);
        login.put("passphrase", passphrase);
        login.put("timestamp", timestamp);
        login.put("sign", signature);
        JSONArray args = JSONArray.of(login);
        return JSONObject.of("op", "login", "args", args);
    }

    public static JSONObject subscribePacket(JSONArray channels) {
        return JSONObject.of("op", "subscribe", "args", channels);
    }

    public static JSONObject placeOrderPacket(TradingProperties properties, String side, PosSide posSide,
                                              String ordType, int pos, double px) {
        JSONObject orderPacket = orderPacket(properties, side, posSide, ordType, pos, px);
        JSONArray args = JSONArray.of(orderPacket);
        JSONObject packet = JSONObject.of("op", "order");
        packet.put("id", Long.toString(identifier()));
        packet.put("args", args);
        return packet;
    }


    public static JSONObject orderPacket(TradingProperties properties, String side, PosSide posSide,
                                         String ordType, int pos, double px) {
        JSONObject data = JSONObject.of("instId", properties.getInstId());
        data.put("tdMode", properties.getTdMode());
        data.put("side", side);
        data.put("posSide", posSide.getName());
        data.put("ordType", ordType);
        data.put("sz", pos);
        if (px > 0)
            data.put("px", Double.toString(px));
        return data;
    }

    /*
    public static JSONObject placeOrdersPacket(Collection<Order> orderList) {
        JSONArray args = new JSONArray();
        orderList.forEach(order -> args.add(orderPacket(order)));
        JSONObject packet = JSONObject.of("op", "batch-orders");
        packet.put("id", Long.toString(identifier()));
        packet.put("args", args);
        return packet;
    }
    */

    public static JSONObject cancelOrdersPacket(List<String> lives, String instId) {
        JSONObject packet = JSONObject.of("id", Long.toString(identifier()), "op", "batch-cancel-orders");
        JSONArray args = new JSONArray();
        lives.forEach(live -> args.add(JSONObject.of("instId", instId, "ordId", live)));
        packet.put("args", args);
        return packet;
    }

}
