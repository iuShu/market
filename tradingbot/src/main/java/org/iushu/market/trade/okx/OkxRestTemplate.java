package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.tomcat.util.buf.StringUtils;
import org.iushu.market.Constants;
import org.iushu.market.component.JSONArrayAdapter;
import org.iushu.market.component.Signature;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OkxComponent
public class OkxRestTemplate {

    private static final Logger logger = LoggerFactory.getLogger(OkxRestTemplate.class);

    private final RestTemplate restTemplate;
    private final TradingProperties properties;
    private final TradingProperties.ApiInfo apiInfo;

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

    private HttpEntity<JSONObject> entity(HttpMethod httpMethod, String api, JSONObject body) {
        String utcTime = utc();
        String content = utcTime + httpMethod.name() + api;
        content += httpMethod == HttpMethod.GET ? toUrlParams(body) : body.toJSONString();
        String signature = Signature.sign(content, apiInfo.getSecret());
        HttpHeaders headers = new HttpHeaders();
        headers.put("OK-ACCESS-SIGN", Collections.singletonList(signature));
        headers.put("OK-ACCESS-TIMESTAMP", Collections.singletonList(utcTime));
        return new HttpEntity<>(body, headers);
    }

    private JSONObject get(String api, HttpEntity<JSONObject> entity) {
        return this.restTemplate.getForObject(url(api), JSONObject.class, entity);
    }

    private JSONObject post(String api, HttpEntity<JSONObject> entity) {
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

    public JSONArray getLeverage() {
        JSONObject body = JSONObject.of("instId", properties.getInstId(), "mgnMode", properties.getTdMode());
        HttpEntity<JSONObject> entity = entity(HttpMethod.GET, OkxConstants.GET_LEVER, body);
        JSONObject response = get(OkxConstants.GET_LEVER, entity);
        if (checkResp(response, "get leverage"))
            return response.getJSONArray("data");
        return JSONArray.of();
    }

    public boolean cancelAlgoOrder(String algoOrderId) {
        JSONObject data = JSONObject.of("instId", properties.getInstId(), "algoId", algoOrderId);
        HttpEntity<JSONObject> entity = entity(HttpMethod.POST, OkxConstants.CANCEL_ALGO, JSONArrayAdapter.of(data));
        JSONObject response = post(OkxConstants.CANCEL_ALGO, entity);
        return checkResp(response, "cancel algo order");
    }

    private static boolean checkResp(JSONObject response, String topic) {
        if (response != null && response.getIntValue("code", -1) == 0)
            return true;
        logger.error("{} failed by {}", topic, response == null ? "null" : response.toJSONString());
        return false;
    }

}
