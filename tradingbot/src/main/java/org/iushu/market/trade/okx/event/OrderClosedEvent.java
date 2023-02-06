package org.iushu.market.trade.okx.event;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.context.ApplicationEvent;

public class OrderClosedEvent extends ApplicationEvent {

    public OrderClosedEvent(JSONObject source) {
        super(source);
    }

}
