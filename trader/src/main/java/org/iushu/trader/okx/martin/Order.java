package org.iushu.trader.okx.martin;

import org.iushu.trader.base.Constants;
import org.iushu.trader.base.PosSide;

import java.util.concurrent.atomic.AtomicInteger;

public class Order {

    private static final int STATE_LIVE = 1;
    private static final int STATE_FILLED = 2;
    private static final int STATE_CANCELED = 3;

    private String side;
    private PosSide posSide;
    private String orderType;
    private int position;

    private volatile String orderId;
    private volatile double price;
    private volatile String algoId;
    private volatile double extraMargin;
    private final AtomicInteger state = new AtomicInteger(STATE_LIVE);

    private long createTime;
    private long updateTime;

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public PosSide getPosSide() {
        return posSide;
    }

    public void setPosSide(PosSide posSide) {
        this.posSide = posSide;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getState() {
        switch (this.state.get()) {
            case STATE_FILLED:
                return Constants.ORDER_STATE_FILLED;
            case STATE_CANCELED:
                return Constants.ORDER_STATE_CANCELED;
            default:
                return Constants.ORDER_STATE_LIVE;
        }
    }

    public void setState(String state) {
        if (Constants.ORDER_STATE_FILLED.equals(state))
            this.state.compareAndSet(STATE_LIVE, STATE_FILLED);
        if (Constants.ORDER_STATE_CANCELED.equals(state))
            if (!this.state.compareAndSet(STATE_LIVE, STATE_CANCELED))
                if (!this.state.compareAndSet(STATE_FILLED, STATE_CANCELED))
                    throw new IllegalStateException("illegal state, " + getState() + " change to " + state);
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getAlgoId() {
        return algoId;
    }

    public void setAlgoId(String algoId) {
        this.algoId = algoId;
    }

    public double getExtraMargin() {
        return extraMargin;
    }

    public void setExtraMargin(double extraMargin) {
        this.extraMargin = extraMargin;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
