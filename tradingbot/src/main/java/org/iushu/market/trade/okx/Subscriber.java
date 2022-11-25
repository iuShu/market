package org.iushu.market.trade.okx;

import java.lang.reflect.Method;

public class Subscriber {

    private Object subscriber;
    private Method handleMethod;

    public Subscriber(Object subscriber, Method handleMethod) {
        this.subscriber = subscriber;
        this.handleMethod = handleMethod;
    }

    public Object getSubscriber() {
        return subscriber;
    }

    public Method getHandleMethod() {
        return handleMethod;
    }

    @Override
    public String toString() {
        return String.format("subscriber=%s, method=%s", subscriber.getClass().getName(), handleMethod.getName());
    }
}
