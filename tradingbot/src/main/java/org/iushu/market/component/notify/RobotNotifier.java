package org.iushu.market.component.notify;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.component.Signature;
import org.iushu.market.config.TradingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("robotNotifier")
@ConditionalOnProperty(name = "trade.notify.robot")
public class RobotNotifier implements Notifier {

    private final RestTemplate restTemplate;
    private final TradingProperties properties;

    public RobotNotifier(RestTemplate restTemplate, TradingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public void notify(String title, String content) {
        JSONObject body = JSONObject.of("title", title, "text", content);
        JSONObject message = JSONObject.of("msgtype", "markdown", "markdown", body);
        send(message);
    }

    private void send(JSONObject message) {
        long timestamp = System.currentTimeMillis();
        String sign = timestamp + "\n" + properties.getNotify().getSecret();
        String signature = Signature.sign(sign, properties.getNotify().getSecret());
        String url = String.format("%s&timestamp=%s&sign=%s", properties.getNotify().getWebhook(), timestamp, signature);
        this.restTemplate.postForObject(url, message, JSONObject.class);
    }

}
