package org.iushu.trader.base;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class DingTalkRobot {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkRobot.class);

    private static HttpClient httpClient;
    private static RequestConfig requestConfig;

    static {
        try {
            requestConfig = RequestConfig.custom().setConnectTimeout(10000)
                    .setConnectionRequestTimeout(7000).setSocketTimeout(10000).build();

            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            httpClient = HttpClients.custom().setSSLSocketFactory(factory).setDefaultRequestConfig(requestConfig).build();
        } catch (Exception e) {
            logger.error("init ssl http client error", e);
            System.exit(1);
        }
    }

    private static JSONObject post(String url, JSONObject body) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(new BasicHeader(CONTENT_TYPE, "application/json; charset=utf-8"));
        httpPost.setConfig(requestConfig);
        try {
            httpPost.setEntity(new StringEntity(body.toJSONString(), UTF_8));
            HttpResponse response = httpClient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            HttpEntity responseEntity = response.getEntity();
            if (code / 100 == 2)
                return JSONObject.parseObject(EntityUtils.toString(responseEntity));
            String resp = responseEntity == null ? "unknown" : EntityUtils.toString(responseEntity);
            logger.error("http post failed at {} with {} {}", url, code, resp);
        } catch (IOException e) {
            logger.error("http post error", e);
        }
        httpPost.releaseConnection();
        return JSONObject.of();
    }

    private static String hmac(long timestamp, String secret) throws Exception {
        String toSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(toSign.getBytes(UTF_8));
        return URLEncoder.encode(new String(Base64.getEncoder().encode(bytes), UTF_8));
    }

    public static void sendText(String content) {
        if (!Configuration.getBoolean("notify.dingtalk.open"))
            return;

//        JSONObject text = JSONObject.of("content", String.format("%s %s", content, currentTime()));
//        JSONObject body = JSONObject.of("msgtype", "text", "text", text);

        String actionUrl = "";
//        String okxAppUrl = "okex://main";
        JSONObject markdown = JSONObject.of("title", "Order Filled", "text", markDown()
        , "btnOrientation", "0", "btns", JSONArray.of(JSONObject.of("title", "Details(okx)", "actionURL", actionUrl)));
        JSONObject body = JSONObject.of("msgtype", "actionCard", "actionCard", markdown);
        logger.info(markdown.toJSONString());

        try {
            String webhook = Configuration.getString("dingtalk.webhook");
            String secret = Configuration.getString("dingtalk.secret");
            long timestamp = System.currentTimeMillis();
            String signature = hmac(timestamp, secret);
            String url = String.format("%s&timestamp=%s&sign=%s", webhook, timestamp, signature);
            post(url, body);
        } catch (Exception e) {
            logger.error("send text error", e);
        }
    }

    public static String markDown() {
        return "#### Order Filled\n" +
                "- 10 16590.1 **FILLED** 2022-11-21 11:36:12.445\n" +
                "- 20 16258.2 live\n" +
                "- 40 15926.4 live\n" +
                "- 80 15428.7 live\n" +
                "\n*tp=19283.4 sl=20129.87*\n" +
                "> " + currentTime();
    }

    private static String currentTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public static void main(String[] args) throws Exception {
        sendText("");
    }

}
