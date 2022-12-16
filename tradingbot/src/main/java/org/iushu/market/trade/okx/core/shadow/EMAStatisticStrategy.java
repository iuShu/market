package org.iushu.market.trade.okx.core.shadow;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.okx.OkxConstants;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.config.OkxShadowComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.springframework.context.annotation.Primary;

@Primary
@OkxShadowComponent
public class EMAStatisticStrategy extends org.iushu.market.trade.okx.core.EMAStatisticStrategy {

    public EMAStatisticStrategy(OkxRestTemplate restTemplate) {
        super(restTemplate);
    }

    @SubscribeChannel(channel = OkxConstants.CHANNEL_CANDLE)
    public void feed(JSONObject message) {
        super.feedTradingData(message);
    }

}
