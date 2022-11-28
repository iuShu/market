package org.iushu.market.component.notify;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.component.Signature;
import org.iushu.market.config.TradingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;

@Component("robotNotifier")
@ConditionalOnProperty(name = "trade.notify.robot")
public class RobotNotifier implements Notifier {

    private final RestTemplate restTemplate;
    private final TradingProperties properties;
    private final TaskScheduler taskScheduler;

    private final Queue<Long> sentQueue = new LinkedList<>();
    private final Queue<JSONObject> pendingQueue = new LinkedList<>();

    public RobotNotifier(RestTemplate restTemplate, TradingProperties properties, TaskScheduler taskScheduler) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void notify(String title, String content) {
        JSONObject body = JSONObject.of("title", title, "text", content);
        JSONObject message = JSONObject.of("msgtype", "markdown", "markdown", body);
        send(message, true);
    }

    private boolean send(JSONObject message, boolean pending) {
        if (rateLimit(message, pending))
            return false;

        if (pendingQueue.size() > 0) {
            JSONObject markdown = message.getJSONObject("markdown");
            String text = markdown.getString("text");
            markdown.put("text", text + " (" + pendingQueue.size() + ")");
        }

        long timestamp = System.currentTimeMillis();
        String sign = timestamp + "\n" + properties.getNotify().getSecret();
        String signature = Signature.sign(sign, properties.getNotify().getSecret());
        String url = String.format("%s&timestamp=%s&sign=%s", properties.getNotify().getWebhook(), timestamp, signature);
        this.restTemplate.postForObject(url, message, JSONObject.class);
        return true;
    }

    private boolean rateLimit(JSONObject message, boolean pending) {
        long now = System.currentTimeMillis();
        if (sentQueue.size() < properties.getNotify().getRateLimited()) {
            sentQueue.offer(now);
            return false;
        }

        Long earliest = sentQueue.peek();
        if (now - earliest >= properties.getNotify().getLimitedPeriod()) {
            sentQueue.poll();
            sentQueue.offer(now);
            return false;
        }

        if (!pending)
            return false;

        pendingQueue.offer(message);
        taskScheduler.scheduleAtFixedRate(() -> {
            if (send(pendingQueue.peek(), false))
                pendingQueue.poll();
        }, Duration.ofMillis(1000));
        return true;
    }

}
