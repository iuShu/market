package org.iushu.market.trade.okx.core.shadow;

import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.config.OkxShadowComponent;

@OkxShadowComponent("shadowStrategy")
public class EMAStrategy extends org.iushu.market.trade.okx.core.EMAStrategy {

    public EMAStrategy(OkxRestTemplate restTemplate) {
        super(restTemplate);
    }

}
