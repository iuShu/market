package org.iushu.market.trade.okx.core.shadow;

import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.okx.OkxConstants;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.config.OkxShadowComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;

@OkxShadowComponent("shadowStrategy")
public class EMAStrategy extends org.iushu.market.trade.okx.core.EMAStrategy {

    public EMAStrategy(OkxRestTemplate restTemplate) {
        super(restTemplate);
    }

    @SubscribeChannel(channel = OkxConstants.CHANNEL_CANDLE)
    public void feedCandle(JSONObject message) {
        super.feedTradingData(message);
    }

}
