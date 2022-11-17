package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
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
        JSONObject response = HttpUtils.post(Setting.API_MARGIN_BALANCE, body);
        return checkResp(response, "add extra margin");
    }

    public static boolean closePosition(PosSide posSide) {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID);
        body.put("posSide", posSide.getName());
        body.put("mgnMode", Setting.TD_MODE);
        body.put("autoCxl", true);
        JSONObject response = HttpUtils.post(Setting.API_CLOSE_POSITION, body);
        return checkResp(response, "close position");
    }

    public static int getLeverage() {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID, "mgnMode", Setting.TD_MODE);
        JSONObject response = HttpUtils.post(Setting.API_LEVERAGE, body);
        if (!checkResp(response, "get leverage"))
            return -1;
        JSONArray data = response.getJSONArray("data");
        return data.getJSONObject(0).getIntValue("lever", -1);
    }

    public static boolean setLeverage(int lever) {
        JSONObject body = JSONObject.of("instId", Setting.INST_ID);
        body.put("lever", lever);
        body.put("mgnMode", Setting.TD_MODE);
        JSONObject response = HttpUtils.post(Setting.API_SET_LEVERAGE, body);
        return checkResp(response, "set leverage");
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
