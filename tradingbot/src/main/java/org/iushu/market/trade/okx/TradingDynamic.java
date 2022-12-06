package org.iushu.market.trade.okx;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.Constants;
import org.iushu.market.component.notify.Notifier;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderErrorEvent;
import org.iushu.market.trade.okx.event.OrderFilledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@Profile(Constants.EXChANGE_OKX)
public class TradingDynamic {

    private static final Logger logger = LoggerFactory.getLogger(TradingDynamic.class);

    private final Notifier notifier;
    private final DispatchManager manager;

    public TradingDynamic(Notifier notifier, DispatchManager manager) {
        this.notifier = notifier;
        this.manager = manager;
    }

    @EventListener(OrderFilledEvent.class)
    public void notifyOrderFilled(OrderFilledEvent event) {
        JSONObject data = (JSONObject) event.getSource();

        String fillPx = data.getString("avgPx");
        String contractSize = data.getString("sz");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        int ttlCs = data.getIntValue("_ttlCs", -1);
        double tpPx = data.getDoubleValue("_tpPx");
        double slPx = data.getDoubleValue("_slPx");

        StringBuilder template = new StringBuilder("#### **Order Filled**\n----\n");
        template.append(String.format("filled at %s %s %s %s\n\n", fillPx, contractSize, posSide.getName(), posSide.openSide()));
        template.append(String.format("%d tp=%s sl=%s\n", ttlCs, tpPx, slPx));
        template.append("\n----\n").append(currentTime());
        logger.info("notify order filled {} {}", fillPx, contractSize);
        notifier.notify("Order Filled", template.toString());
    }

    @EventListener(OrderClosedEvent.class)
    public void notifyOrderClosed(OrderClosedEvent event) {
        JSONObject data = (JSONObject) event.getSource();
        String template = "#### **Order Closed**\n----\n" +
                String.format("closed at %s %s\n\n", data.getString("fillSz"), data.getString("fillPx")) +
                "pnl " + data.getString("pnl") + "\n\n----\n" + currentTime();
        notifier.notify("Order Closed", template);
    }

    @EventListener(OrderErrorEvent.class)
    public void notifyOrderError(OrderErrorEvent event) {
        Object source = event.getSource();
        String template = "#### **Order Error**\n----\n\n%s\n\n----\n" + currentTime();
        notifier.notify("Order Error", String.format(template, source.toString()));
        manager.close();
    }

    public static String currentTime() {
        return formatTimestamp(System.currentTimeMillis());
    }

    public static String formatTimestamp(long timestamp) {
        Instant instant = Long.toString(timestamp).length() == 10 ? Instant.ofEpochSecond(timestamp) : Instant.ofEpochMilli(timestamp);
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}
