package org.iushu.trader.okx.martin;

import org.iushu.trader.base.PosSide;

public interface Strategy<T> {

    void prepare();

    void feed(T data);

    boolean satisfy(T data);

    default PosSide decideSide(T data) {
        return null;
    }

}
