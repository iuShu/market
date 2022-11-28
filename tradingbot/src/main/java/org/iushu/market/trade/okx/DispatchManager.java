package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.client.ChannelWebSocketHandler;
import org.iushu.market.client.event.ChannelMessagingEvent;
import org.iushu.market.client.event.ChannelOpenedEvent;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.iushu.market.trade.okx.OkxConstants.*;

@OkxComponent
public class DispatchManager {

    private static final Logger logger = LoggerFactory.getLogger(DispatchManager.class);

    private final TradingProperties properties;
    private final TradingProperties.ApiInfo apiInfo;
    private final Map<String, List<Subscriber>> operations = new HashMap<>();
    private final Map<String, List<Subscriber>> events = new HashMap<>();
    private final Map<String, List<Subscriber>> channels = new HashMap<>();

    private OkxWebSocketSession publicSession;
    private OkxWebSocketSession privateSession;
    private ConfigurableApplicationContext applicationContext;

    public DispatchManager(TradingProperties properties, TradingProperties.ApiInfo apiInfo) {
        this.properties = properties;
        this.apiInfo = apiInfo;
    }

    @Async
    @EventListener(ChannelOpenedEvent.class)
    public void onChannelOpen(ChannelOpenedEvent<StandardWebSocketSession> event) {
        ChannelWebSocketHandler handler = (ChannelWebSocketHandler) event.getSource();
        OkxWebSocketSession session = new OkxWebSocketSession(event.getPayload());

        JSONObject packet;
        if (apiInfo.getWsPrivateUrl().equals(handler.getWebsocketUrl())) {
            privateSession = session;
            packet = PacketUtils.loginPacket(apiInfo.getApiKey(), apiInfo.getSecret(), apiInfo.getPassphrase());
        }
        else {
            publicSession = session;
            packet = subscribeChannelPacket(session);
        }

        if (session.sendMessage(packet))
            logger.debug("sent message {}", packet.toJSONString());
        else
            close();
    }

    @Async
    @EventListener(ChannelMessagingEvent.class)
    public void handleMessage(ChannelMessagingEvent<JSONObject> event) {
        OkxWebSocketSession session = new OkxWebSocketSession((WebSocketSession) event.getSource());
        JSONObject message = event.getPayload();
        dispatchOperation(session, message);
        dispatchEvent(session, message);
        dispatchChannel(session, message);
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
        JSONArray data = message.getJSONArray("data");
        if (data == null || data.isEmpty())
            return;

        try {
            JSONObject arg = message.getJSONObject("arg");
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
        JSONObject packet = subscribeChannelPacket(session);
        if (session.sendMessage(packet))
            logger.debug("sent message {}", packet.toJSONString());
        else
            close();
    }

    @SubscribeChannel(event = EVENT_SUBSCRIBE)
    public void subscribeResponse(JSONObject message) {
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
        if (publicSession != null && publicSession.isActive())
            publicSession.close();
        if (privateSession != null && privateSession.isActive())
            privateSession.close();
        this.applicationContext.close();
    }

    private JSONObject subscribeChannelPacket(OkxWebSocketSession session) {
        JSONArray subscribingChannels = JSONArray.of();
        boolean isPrivate = privateSession != null && session.getSessionId().equals(privateSession.getSessionId());
        channels.keySet().stream().filter(channel -> isPrivate == PRIVATE_CHANNELS.contains(channel)).forEach(k -> {
            JSONObject each = JSONObject.of();
            each.put("channel", k);
            each.put("instId", properties.getInstId());
            each.put("instType", properties.getInstType());
            subscribingChannels.add(each);
        });
        return subscribingChannels.isEmpty() ? JSONObject.of() : PacketUtils.subscribePacket(subscribingChannels);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scanSubscriber(ApplicationReadyEvent readyEvent) {
        try {
            this.applicationContext = readyEvent.getApplicationContext();
            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(applicationContext);
            MetadataReaderFactory factory = new CachingMetadataReaderFactory(resolver);
            Resource[] resources = resolver.getResources("classpath:**/*.class");
            String annotationName = SubscribeChannel.class.getCanonicalName();
            for (Resource resource : resources) {
                MetadataReader reader = factory.getMetadataReader(resource);
                AnnotationMetadata metadata = reader.getAnnotationMetadata();
                Set<MethodMetadata> annotatedMethods = metadata.getAnnotatedMethods(annotationName);
                if (!metadata.getAnnotations().isPresent(OkxComponent.class) || annotatedMethods.isEmpty())
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
