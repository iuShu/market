package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.Constants;
import org.iushu.market.client.ChannelWebSocketHandler;
import org.iushu.market.client.event.ChannelClosedEvent;
import org.iushu.market.client.event.ChannelMessagingEvent;
import org.iushu.market.client.event.ChannelOpenedEvent;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.OkxShadowComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.iushu.market.trade.okx.OkxConstants.*;

@Component
@Profile(Constants.EXChANGE_OKX)
public class DispatchManager {

    private static final Logger logger = LoggerFactory.getLogger(DispatchManager.class);

    private final TradingProperties properties;
    private final TradingProperties.ApiInfo apiInfo;
    private final Map<String, List<Subscriber>> operations = new HashMap<>();
    private final Map<String, List<Subscriber>> events = new HashMap<>();
    private final Map<String, List<Subscriber>> channels = new HashMap<>();

    private Class annotatedClass = null;
    private final OkxWebSocketSession session = new OkxWebSocketSession();
    private ConfigurableApplicationContext applicationContext;
    private final CountDownLatch latch = new CountDownLatch(2);

    public DispatchManager(TradingProperties properties, TradingProperties.ApiInfo apiInfo) {
        this.properties = properties;
        this.apiInfo = apiInfo;
    }

    @Async
    @EventListener(ChannelOpenedEvent.class)
    public void onChannelOpen(ChannelOpenedEvent<StandardWebSocketSession> event) {
        ChannelWebSocketHandler handler = (ChannelWebSocketHandler) event.getSource();
        WebSocketSession session = event.getPayload();

        JSONObject packet;
        if (apiInfo.getWsPrivateUrl().equals(handler.getWebsocketUrl())) {
            this.session.setPrivateSession(session);
            packet = PacketUtils.loginPacket(apiInfo.getApiKey(), apiInfo.getSecret(), apiInfo.getPassphrase());
            this.session.sendPrivateMessage(packet);
        }
        else {
            this.session.setPublicSession(session);
            waitOther();
            packet = subscribeChannelPacket(false);
            this.session.sendPublicMessage(packet);
        }
    }

    private void waitOther() {
        try {
            if (latch.getCount() == 2) {
                latch.countDown();
                latch.await();
            }
            if (latch.getCount() == 1)
                latch.countDown();
        } catch (InterruptedException e) {
            logger.error("wait other error", e);
            System.exit(1);
        }
    }

    @Async
    @EventListener(ChannelMessagingEvent.class)
    public void handleMessage(ChannelMessagingEvent<JSONObject> event) {
        JSONObject message = event.getPayload();
        dispatchOperation(session, message);
        dispatchEvent(session, message);
        dispatchChannel(session, message);
    }

    @Async
    @EventListener(ChannelClosedEvent.class)
    public void channelClosed() {
        this.close();
    }

    private void dispatchOperation(OkxWebSocketSession session, JSONObject message) {
        String op = message.getString("op");
        if (op != null && !op.isEmpty())
            invokeSubscriber(session, operations.get(op), message);
    }

    private void dispatchEvent(OkxWebSocketSession session, JSONObject message) {
        String event = message.getString("event");
        if (event != null && !event.isEmpty())
            invokeSubscriber(session, events.get(event), message);
    }

    private void dispatchChannel(OkxWebSocketSession session, JSONObject message) {
        JSONObject arg = message.getJSONObject("arg");
        JSONArray data = message.getJSONArray("data");
        if (data == null || arg == null)
            return;

        try {
            String channel = arg.getString("channel");
            invokeSubscriber(session, channels.get(channel), message);
        } catch (Exception e) {
            logger.error("decode message error {}", message, e);
            close();
        }
    }

    @SubscribeChannel(event = EVENT_LOGIN)
    public void loginResponse(OkxWebSocketSession session, JSONObject message) {
        if (SUCCESS != message.getIntValue("code", -1)) {
            logger.error("shutdown due to login failed {}", message.toJSONString());
            close();
            return;
        }

        logger.info("login success");
        JSONObject packet = subscribeChannelPacket(true);
        if (session.sendPrivateMessage(packet))
            logger.debug("sent message {}", packet.toJSONString());
        else
            close();
    }

    @SubscribeChannel(event = EVENT_SUBSCRIBE)
    public void subscribeResponse(JSONObject message) {
        waitOther();
        JSONObject arg = message.getJSONObject("arg");
        logger.info("subscribed to channel {}", arg.getString("channel"));
    }

    @SubscribeChannel(event = EVENT_ERROR)
    public void errorResponse(JSONObject message) {
        logger.error("shutdown due to operation error {}", message.toJSONString());
        close();
    }

    private void invokeSubscriber(OkxWebSocketSession session, List<Subscriber> subscribers, JSONObject message) {
        if (subscribers == null || subscribers.isEmpty()) {
            logger.warn("no subscriber for {}", message.toJSONString());
            return;
        }

        subscribers.forEach(subscriber -> {
            try {
                Object bean = subscriber.getSubscriber();
                Method handler = subscriber.getHandleMethod();
                Object[] args = handler.getParameterCount() == 1 ? new Object[]{message} : new Object[]{session, message};
                handler.invoke(bean, args);
            } catch (Exception e) {
                logger.error("invoke subscriber method error {}", message.toJSONString(), e);
                close();
            }
        });
    }

    public void close() {
        this.session.close();
        this.applicationContext.close();
    }

    private JSONObject subscribeChannelPacket(boolean isPrivate) {
        JSONArray subscribingChannels = JSONArray.of();
        channels.keySet().stream().filter(channel -> isPrivate == PRIVATE_CHANNELS.contains(channel)).forEach(k -> {
            JSONObject each = JSONObject.of();
            each.put("channel", k);
            each.put("instId", properties.getInstId());
            each.put("instType", properties.getInstType());
            subscribingChannels.add(each);
        });
        return subscribingChannels.isEmpty() ? JSONObject.of() : PacketUtils.subscribePacket(subscribingChannels);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void scanSubscriber(ContextRefreshedEvent refreshedEvent) {
        try {
            this.applicationContext = (ConfigurableApplicationContext) refreshedEvent.getApplicationContext();
            for (String activeProfile : this.applicationContext.getEnvironment().getActiveProfiles()) {
                if (activeProfile.equals("shadow")) {
                    annotatedClass = OkxShadowComponent.class;
                    break;
                }
            }
            annotatedClass = annotatedClass == null ? OkxComponent.class : annotatedClass;

            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(applicationContext);
            MetadataReaderFactory factory = new CachingMetadataReaderFactory(resolver);
            Resource[] resources = resolver.getResources("classpath*:org/iushu/market/trade/**/*.class");
            String annotationName = SubscribeChannel.class.getCanonicalName();
            for (Resource resource : resources) {
                MetadataReader reader = factory.getMetadataReader(resource);
                AnnotationMetadata metadata = reader.getAnnotationMetadata();
                Set<MethodMetadata> annotatedMethods = metadata.getAnnotatedMethods(annotationName);
                if (!reader.getAnnotationMetadata().getClassName().equals(DispatchManager.class.getName())
                    && (!metadata.getAnnotations().isPresent(annotatedClass) || annotatedMethods.isEmpty()))
                    continue;

                Class<?> beanClass = Class.forName(metadata.getClassName());
                Object bean = applicationContext.getBean(beanClass);
                annotatedMethods.forEach(annotatedMethod -> {
                    List<Method> methods = Arrays.stream(beanClass.getMethods()).filter(method -> method.getName().equals(annotatedMethod.getMethodName())).collect(Collectors.toList());
                    methods.forEach(method -> {
                        SubscribeChannel annotation = method.getAnnotation(SubscribeChannel.class);
                        Subscriber subscriber = new Subscriber(bean, method);
                        Arrays.stream(annotation.op()).filter(o -> !o.equals(""))
                                .forEach(op -> operations.computeIfAbsent(op, k -> new ArrayList<>()).add(subscriber));
                        Arrays.stream(annotation.event()).filter(e -> !e.equals(""))
                                .forEach(event -> events.computeIfAbsent(event, k -> new ArrayList<>()).add(subscriber));
                        Arrays.stream(annotation.channel()).filter(c -> !c.equals(""))
                                .forEach(channel -> channels.computeIfAbsent(channel, k -> new ArrayList<>()).add(subscriber));
                    });
                });
            }
//            operations.forEach((k, v) -> System.out.println(k + " " + v));
//            events.forEach((k, v) -> System.out.println(k + " " + v));
//            channels.forEach((k, v) -> System.out.println(k + " " + v));
        } catch (Exception e) {
            logger.error("load subscribing method error", e);
            System.exit(1);
        }
    }

}
