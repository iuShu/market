package org.iushu.trader.okx.martin;

public interface Strategy<T> {

    void prepare();

    void feed(T data);

    boolean satisfy(T data);

}
