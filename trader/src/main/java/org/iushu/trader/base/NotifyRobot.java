package org.iushu.trader.base;

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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

public class NotifyRobot {

    private static final Logger logger = LoggerFactory.getLogger(NotifyRobot.class);

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

    private static void send(JSONObject message) {
        if (message == null || message.isEmpty() || !Configuration.getBoolean("notify.robot.open"))
            return;

        try {
            logger.debug("robot send {}", message.toJSONString());
            String webhook = Configuration.getString("notify.robot.webhook");
            String secret = Configuration.getString("notify.robot.secret");
            long timestamp = System.currentTimeMillis();
            String signature = hmac(timestamp, secret);
            String url = String.format("%s&timestamp=%s&sign=%s", webhook, timestamp, signature);
            post(url, message);
        } catch (Exception e) {
            logger.error("send text error", e);
        }
    }

    /**
     * first order has been filled (more detailed) <br>
     * follow orders has been filled <br>
     * order has been closed <br>
     * trader shutdown <br>
     */
    public static void sendText(String content) {
        JSONObject text = JSONObject.of("content", String.format("%s %s", content, currentTime()));
        JSONObject message = JSONObject.of("msgtype", "text", "text", text);
        send(message);
    }

    public static void sendMarkdown(String title, String content) {
        JSONObject markdown = JSONObject.of("title", title, "text", content + "\n\n----\n" + currentTime());
        JSONObject message = JSONObject.of("msgtype", "markdown", "markdown", markdown);
        send(message);
    }

    public static void sendFatal(String content) {
        sendMarkdown("FATAL", content);
    }

    public static String messageTemplate() {
        return "#### **Order Filled**\n----\n" +
                "> - 10 16590.1 **Fill** " + currentTime().substring(5) + "\n" +
                "> - 20 16258.2 live\n" +
                "> - 40 15926.4 live\n" +
                "> - 80 15428.7 live\n" +
                "\n*tp=19283.4 sl=20129.87*\n\n----\n" + currentTime();
    }

    public static String currentTime() {
        return formatTimestamp(System.currentTimeMillis());
    }

    public static String formatTimestamp(long timestamp) {
        Instant instant = Long.toString(timestamp).length() == 10 ? Instant.ofEpochSecond(timestamp) : Instant.ofEpochMilli(timestamp);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String periodDesc(long timestamp) {
        Instant instant = Long.toString(timestamp).length() == 10 ? Instant.ofEpochSecond(timestamp) : Instant.ofEpochMilli(timestamp);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        int period = now.getDayOfYear() - time.getDayOfYear();
        String desc = period == 0 ? "" : (period + "d ");
        return desc + time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static void main(String[] args) throws Exception {
//        sendText("");
    }

}
