package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.base.JSONArrayAdapter;
import org.iushu.trader.base.PosSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkxHttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(OkxHttpUtils.class);

    public static JSONArray getCandleHistory(String bar, int limit) {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID);
        body.put("bar", bar);
        body.put("limit", Integer.toString(limit));
        JSONObject response = HttpUtils.get(Setting.API_CANDLE_DATA, body);
        if (checkResp(response, "get candle history"))
            return response.getJSONArray("data");
        return JSONArray.of();
    }

    public static boolean addExtraMargin(PosSide posSide, double amount) {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID);
        body.put("posSide", posSide.getName());
        body.put("type", "add");
        body.put("amt", Double.toString(amount));
        return checkResp(HttpUtils.post(Setting.API_MARGIN_BALANCE, body), "add extra margin");
    }

    public static JSONObject placeAlgoOrder(PosSide posSide, String side, int size, double tpPrice, double slPrice) {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID);
        body.put("tdMode", Setting.TD_MODE);
        body.put("posSide", posSide.getName());
        body.put("side", side);
        body.put("ordType", Constants.ALGO_TYPE_OCO);
        body.put("sz", String.valueOf(size));
        body.put("tpTriggerPxType", Constants.ALGO_PX_TYPE_LAST);
        body.put("tpTriggerPx", Double.toString(tpPrice));
        body.put("tpOrdPx", "-1");
        body.put("slTriggerPxType", Constants.ALGO_PX_TYPE_LAST);
        body.put("slTriggerPx", Double.toString(slPrice));
        body.put("slOrdPx", "-1");
        JSONObject response = HttpUtils.post(Setting.API_ALGO_ORDER, body);
        if (checkResp(response, "place algo order"))
            return response.getJSONArray("data").getJSONObject(0);
        return JSONObject.of();
    }

    public static boolean cancelAlgoOrder(String algoOrderId) {
        JSONObject data = JSONObject.of("instId", Setting.INST_ID, "algoId", algoOrderId);
        JSONArrayAdapter body = new JSONArrayAdapter(JSONArray.of(data));
        return checkResp(HttpUtils.post(Setting.API_CANCEL_ALGO, body), "cancel algo order");
    }

    public static boolean closePosition(PosSide posSide) {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID);
        body.put("posSide", posSide.getName());
        body.put("mgnMode", Setting.TD_MODE);
        body.put("autoCxl", true);
        return checkResp(HttpUtils.post(Setting.API_CLOSE_POSITION, body), "close position");
    }

    public static JSONArray getLeverage() {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID, "mgnMode", Setting.TD_MODE);
        JSONObject response = HttpUtils.get(Setting.API_LEVERAGE, body);
        if (checkResp(response, "get leverage"))
            return response.getJSONArray("data");
        return JSONArray.of();
    }

    public static boolean setLeverage(PosSide posSide, int lever) {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID);
        body.put("lever", lever);
        body.put("mgnMode", Setting.TD_MODE);
        body.put("posSide", posSide.getName());
        return checkResp(HttpUtils.post(Setting.API_SET_LEVERAGE, body), "set leverage");
    }

    public static double getBalance() {
        JSONObject body = JSONObject.of("ccy", Setting.CURRENCY);
        JSONObject response = HttpUtils.post(Setting.API_BALANCE, body);
        if (!checkResp(response, "get balance of " + Setting.CURRENCY))
            return 0.0;
        JSONArray data = response.getJSONArray("data");
        JSONArray details = data.getJSONObject(0).getJSONArray("details");
        return details.getJSONObject(0).getDoubleValue("availEq");
    }

    private static boolean checkResp(JSONObject response, String topic) {
        if (response != null && response.getIntValue("code", -1) == 0)
            return true;
        logger.error("{} failed by {}", topic, response == null ? "null" : response.toJSONString());
        return false;
    }

}
