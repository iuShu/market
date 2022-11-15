package org.iushu.trader.okx.martin;

import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.OkxMessageConsumer;

public interface Strategy<T> extends OkxMessageConsumer {

    void prepare();

    void feed(T data);

    boolean satisfy(T data);

    default PosSide decideSide(T data) {
        return null;
    }

}
