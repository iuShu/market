package org.iushu.trader.okx;

import com.alibaba.fastjson2.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.buf.StringUtils;
import org.iushu.trader.base.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private static HttpClient httpClient;
    private static RequestConfig requestConfig;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    private static final String HEADER_APIKEY = "OK-ACCESS-KEY";
    private static final String HEADER_SIGN = "OK-ACCESS-SIGN";
    private static final String HEADER_TIMESTAMP = "OK-ACCESS-TIMESTAMP";
    private static final String HEADER_PASSPHRASE = "OK-ACCESS-PASSPHRASE";

    static {
        try {
            requestConfig = RequestConfig.custom().setConnectTimeout(10000)
                    .setConnectionRequestTimeout(7000).setSocketTimeout(10000).build();

            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            httpClient = HttpClients.custom().setSSLSocketFactory(factory).setDefaultRequestConfig(requestConfig).build();
//            httpClient = HttpClients.createDefault();
        } catch (Exception e) {
            logger.error("init ssl http client error", e);
            System.exit(1);
        }
    }

    public static Header[] headers(String method, String requestPath, JSONObject body) {
        String utc = utc();
        String content = utc + method + requestPath;
        content += method.equals(METHOD_GET) ? toUrlParams(body) : body.toJSONString();
        String signature = Signature.sign(content, Setting.SECRETKEY);

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(HEADER_APIKEY, Setting.APIKEY));
        headers.add(new BasicHeader(HEADER_SIGN, signature));
        headers.add(new BasicHeader(HEADER_TIMESTAMP, utc));
        headers.add(new BasicHeader(HEADER_PASSPHRASE, Setting.PASSPHRASE));
        headers.add(new BasicHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType()));
        if (Setting.TEST)
            headers.add(new BasicHeader("x-simulated-trading", "1"));
        return headers.toArray(new Header[0]);
    }

    public static String toUrlParams(JSONObject params) {
        List<String> parameters = new ArrayList<>(params.size());
        params.forEach((k, v) -> parameters.add(k + "=" + v));
        return "?" + StringUtils.join(parameters, '&');
    }

    public static JSONObject get(String api, JSONObject body) {
        HttpGet httpGet = new HttpGet(Setting.API_URL + api + toUrlParams(body));
        httpGet.setHeaders(headers(METHOD_GET, api, body));
        httpGet.setConfig(requestConfig);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            int code = response.getStatusLine().getStatusCode();
            if (code / 100 == 2)
                return JSONObject.parseObject(EntityUtils.toString(responseEntity));

            String resp = responseEntity == null ? "unknown" : EntityUtils.toString(responseEntity);
            logger.error("http get failed with {} {}", code, resp);
        } catch (IOException e) {
            logger.error("http get error", e);
        }
        httpGet.releaseConnection();
        return JSONObject.of();
    }

    public static JSONObject post(String api, JSONObject body) {
        HttpPost httpPost = new HttpPost(Setting.API_URL + api);
        httpPost.setHeaders(headers(METHOD_POST, api, body));
        httpPost.setConfig(requestConfig);
        try {
            httpPost.setEntity(new StringEntity(body.toJSONString()));
            HttpResponse response = httpClient.execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            HttpEntity responseEntity = response.getEntity();
            if (code / 100 == 2)
                return JSONObject.parseObject(EntityUtils.toString(responseEntity));
            String resp = responseEntity == null ? "unknown" : EntityUtils.toString(responseEntity);
            logger.error("http post failed with {} {}", code, resp);
        } catch (IOException e) {
            logger.error("http post error", e);
        }
        httpPost.releaseConnection();
        return JSONObject.of();
    }

    public static String utc() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    public static void main(String[] args) throws Exception {
//        System.out.println(utc());
//        System.out.println(Double.toString(0.15));
    }

}
