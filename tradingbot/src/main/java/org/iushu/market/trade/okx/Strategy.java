package org.iushu.market.trade.okx;

import org.iushu.market.trade.PosSide;

public interface Strategy<T> {

    PosSide trend(T base);

}
