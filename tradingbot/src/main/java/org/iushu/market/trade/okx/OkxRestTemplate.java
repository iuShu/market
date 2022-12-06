package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.tomcat.util.buf.StringUtils;
import org.iushu.market.Constants;
import org.iushu.market.component.JSONArrayAdapter;
import org.iushu.market.component.Signature;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.iushu.market.Constants.ALGO_PX_TYPE_LAST;
import static org.iushu.market.Constants.ALGO_TYPE_OCO;

@Component
@Profile(Constants.EXChANGE_OKX)
public class OkxRestTemplate implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(OkxRestTemplate.class);

    private final RestTemplate restTemplate;
    private final TradingProperties properties;
    private final TradingProperties.ApiInfo apiInfo;
    private boolean test = true;

    public OkxRestTemplate(RestTemplate restTemplate, TradingProperties properties, TradingProperties.ApiInfo apiInfo) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.apiInfo = apiInfo;
    }

    private String url(String api) {
        return apiInfo.getApi() + api;
    }

    private static String toUrlParams(JSONObject params) {
        List<String> parameters = new ArrayList<>(params.size());
        params.forEach((k, v) -> parameters.add(k + "=" + v));
        return "?" + StringUtils.join(parameters, '&');
    }

    public static String utc() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    private HttpHeaders headers(HttpMethod httpMethod, String api, Object body) {
        String utcTime = utc();
        String content = utcTime + httpMethod.name() + api;
        content += httpMethod == HttpMethod.GET ? toUrlParams((JSONObject) body) : body.toString();
        String signature = Signature.sign(content, apiInfo.getSecret());
        HttpHeaders headers = new HttpHeaders();
        headers.put("OK-ACCESS-SIGN", Collections.singletonList(signature));
        headers.put("OK-ACCESS-TIMESTAMP", Collections.singletonList(utcTime));
        headers.put("OK-ACCESS-KEY", Collections.singletonList(apiInfo.getApiKey()));
        headers.put("OK-ACCESS-PASSPHRASE", Collections.singletonList(apiInfo.getPassphrase()));
        headers.put("Content-Type", Collections.singletonList("application/json;charset=utf-8"));
        if (test)
            headers.put("x-simulated-trading", Collections.singletonList("1"));
        return headers;
    }

    private HttpEntity<JSONArray> entity(HttpMethod httpMethod, String api, JSONArray body) {
        return new HttpEntity<>(body, headers(httpMethod, api, body));
    }

    private HttpEntity<JSONObject> entity(HttpMethod httpMethod, String api, JSONObject body) {
        return new HttpEntity<>(body, headers(httpMethod, api, body));
    }

    private JSONObject get(String api, HttpEntity<JSONObject> entity) {
        return this.restTemplate.exchange(url(api) + toUrlParams(entity.getBody()), HttpMethod.GET, entity, JSONObject.class).getBody();
    }

    private JSONObject post(String api, HttpEntity entity) {
        return this.restTemplate.postForObject(url(api), entity, JSONObject.class);
    }

    public JSONArray getCandleHistory(String bar, int limit) {
        JSONObject body = JSONObject.of("instId", properties.getInstId());
        body.put("bar", bar);
        body.put("limit", Integer.toString(limit));
        HttpEntity<JSONObject> entity = entity(HttpMethod.GET, OkxConstants.GET_CANDLE_HISTORY, body);
        JSONObject response = get(OkxConstants.GET_CANDLE_HISTORY, entity);
        if (checkResp(response, "get candle history"))
            return response.getJSONArray("data");
        return JSONArray.of();
    }

    public double getBalance() {
        JSONObject body = JSONObject.of("ccy", properties.getCurrency());
        HttpEntity<JSONObject> entity = entity(HttpMethod.GET, OkxConstants.GET_BALANCE, body);
        JSONObject response = get(OkxConstants.GET_BALANCE, entity);
        if (!checkResp(response, "get balance of " + properties.getCurrency()))
            return 0.0;
        JSONArray data = response.getJSONArray("data");
        JSONArray details = data.getJSONObject(0).getJSONArray("details");
        return details.getJSONObject(0).getDoubleValue("availEq");
    }

    public JSONArray getLeverage() {
        JSONObject body = JSONObject.of("instId", properties.getInstId(), "mgnMode", properties.getTdMode());
        HttpEntity<JSONObject> entity = entity(HttpMethod.GET, OkxConstants.GET_LEVER, body);
        JSONObject response = get(OkxConstants.GET_LEVER, entity);
        if (checkResp(response, "get leverage"))
            return response.getJSONArray("data");
        return JSONArray.of();
    }

    public boolean setLeverage(PosSide posSide, int lever) {
        JSONObject body = JSONObject.of("instId", properties.getInstId());
        body.put("lever", lever);
        body.put("mgnMode", properties.getTdMode());
        body.put("posSide", posSide.getName());
        HttpEntity<JSONObject> entity = entity(HttpMethod.POST, OkxConstants.SET_LEVER, body);
        return checkResp(post(OkxConstants.SET_LEVER, entity), "set leverage");
    }

    public JSONObject placeAlgoOrder(PosSide posSide, String side, int totalContractSize, double tpPrice, double slPrice) {
        JSONObject body = JSONObject.of("instId", properties.getInstId());
        body.put("tdMode", properties.getTdMode());
        body.put("posSide", posSide.getName());
        body.put("side", side);
        body.put("ordType", ALGO_TYPE_OCO);
        body.put("sz", String.valueOf(totalContractSize));
        body.put("tpTriggerPxType", ALGO_PX_TYPE_LAST);
        body.put("tpTriggerPx", Double.toString(tpPrice));
        body.put("tpOrdPx", "-1");
        body.put("slTriggerPxType", ALGO_PX_TYPE_LAST);
        body.put("slTriggerPx", Double.toString(slPrice));
        body.put("slOrdPx", "-1");
        HttpEntity<JSONObject> entity = entity(HttpMethod.POST, OkxConstants.PLACE_ALGO, body);
        JSONObject response = post(OkxConstants.PLACE_ALGO, entity);
        if (checkResp(response, "place algo order"))
            return response.getJSONArray("data").getJSONObject(0);
        return JSONObject.of();
    }

    public boolean cancelAlgoOrder(String algoOrderId) {
        JSONObject data = JSONObject.of("instId", properties.getInstId(), "algoId", algoOrderId);
        HttpEntity<JSONArray> entity = entity(HttpMethod.POST, OkxConstants.CANCEL_ALGO, JSONArray.of(data));
        JSONObject response = post(OkxConstants.CANCEL_ALGO, entity);
        return checkResp(response, "cancel algo order");
    }

    public boolean closePosition(PosSide posSide) {
        JSONObject body = JSONObject.of("instId", properties.getInstId());
        body.put("posSide", posSide.getName());
        body.put("mgnMode", properties.getTdMode());
        body.put("autoCxl", true);
        HttpEntity<JSONObject> entity = entity(HttpMethod.POST, OkxConstants.CLOSE_POSITION, body);
        JSONObject response = post(OkxConstants.CLOSE_POSITION, entity);
        return checkResp(response, "close position");
    }

    public boolean addExtraMargin(PosSide posSide, double amount) {
        JSONObject body = JSONObject.of("instId", properties.getInstId());
        body.put("posSide", posSide.getName());
        body.put("type", "add");
        body.put("amt", Double.toString(amount));
        HttpEntity<JSONObject> entity = entity(HttpMethod.POST, OkxConstants.ADD_MARGIN, body);
        JSONObject response = post(OkxConstants.ADD_MARGIN, entity);
        return checkResp(response, "add extra margin");
    }

    private static boolean checkResp(JSONObject response, String topic) {
        if (response != null && response.getIntValue("code", -1) == 0)
            return true;
        logger.error("{} failed by {}", topic, response == null ? "null" : response.toJSONString());
        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        test = 1 == Arrays.stream(applicationContext.getEnvironment().getActiveProfiles()).filter(p -> p.equals("test")).count();
    }
}
