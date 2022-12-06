package org.iushu.market.trade.okx.core.shadow;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.config.TradingProperties;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.config.OkxShadowComponent;
import org.iushu.market.trade.okx.event.OrderClosedEvent;
import org.iushu.market.trade.okx.event.OrderFilledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import static org.iushu.market.trade.MartinOrderUtils.*;

@OkxShadowComponent("shadowGuard")
public class Guard {

    private static final Logger logger = LoggerFactory.getLogger(Guard.class);

    private final TradingProperties properties;
    private volatile double firstPx = 0.0;

    public Guard(TradingProperties properties) {
        this.properties = properties;
    }

    @EventListener(OrderFilledEvent.class)
    public void onOrderFilled(OrderFilledEvent event) {
        JSONObject data = (JSONObject) event.getSource();
        int size = data.getIntValue("sz");
        double avgPx = data.getDoubleValue("avgPx");
        PosSide posSide = PosSide.of(data.getString("posSide"));
        logger.info("filled order at {} {} {}", avgPx, posSide.getName(), size);
        if (size == properties.getOrder().getFirstContractSize())
            firstPx = avgPx;

        int ttlCs = totalContractSize(size, properties.getOrder());
        double tpPx = takeProfitPrice(firstPx, size, posSide, properties.getOrder());
        double slPx = stopLossPrice(firstPx, posSide, properties.getOrder());
        logger.info("placed algo order {} tp={} sl={}", ttlCs, tpPx, slPx);
    }

    @EventListener(OrderClosedEvent.class)
    public void onOrderClosed() {
        firstPx = 0.0;
    }

}
