package org.iushu.trader.okx.martin.version2;

import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.okx.martin.Order;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.iushu.trader.base.Constants.*;
import static org.iushu.trader.okx.Setting.*;

public class MartinOrders {

    private final int maxOrder;
    private final List<BigDecimal> followRates = new ArrayList<>(ORDER_MAX_ORDERS);
    private final AtomicInteger batch = new AtomicInteger(1);
    private final AtomicInteger current = new AtomicInteger(0);
    private final Map<Integer, Order> orders;
    private final Map<Integer, Integer> posToIdx;
    private final AtomicBoolean resetting = new AtomicBoolean(false);

    private final BigDecimal ONE_THOUSAND = new BigDecimal("1000");
    private final BigDecimal ORDER_LEVER = new BigDecimal(Setting.ORDER_LEVER);
    private final MathContext DEFAULT_MATH_CONTEXT = new MathContext(10);

    private static final MartinOrders INSTANCE = new MartinOrders();

    private MartinOrders() {
        this.maxOrder = ORDER_MAX_ORDERS;
        this.orders = new ConcurrentHashMap<>(maxOrder);
        this.posToIdx = new ConcurrentHashMap<>(maxOrder);
        prepareOrders();
        this.followRates.add(new BigDecimal("0.01"));
        this.followRates.add(new BigDecimal("0.02"));
        this.followRates.add(new BigDecimal("0.04"));
        this.followRates.add(new BigDecimal("0.07"));
    }

    private void prepareOrders() {
        Order first = new Order();
        first.setPosition(ORDER_START_POS);
        first.setOrderType(ORDER_TYPE_MARKET);
        orders.put(1, first);
        posToIdx.put(first.getPosition(), 1);
        Order prev = first;
        for (int i = 2; i <= this.maxOrder; i++) {
            Order nextOrder = new Order();
            nextOrder.setPosition(prev.getPosition() * 2);
            nextOrder.setOrderType(ORDER_TYPE_LIMIT);
            orders.put(i, nextOrder);
            posToIdx.put(nextOrder.getPosition(), i);
            prev = nextOrder;
        }
    }

    public static MartinOrders instance() {
        return INSTANCE;
    }

    public void setPosSide(int batch, PosSide posSide) {
        if (this.batch.get() != batch)
            throw new IllegalStateException(String.format("obsoleted batch %d of %d", batch, this.batch.get()));
        this.orders.values().forEach(each -> {
            each.setPosSide(posSide);
            each.setSide(posSide == PosSide.LongSide ? SIDE_BUY : SIDE_SELL);
        });
    }

    public void setCurrent(Order order) {
        Integer idx = this.posToIdx.get(order.getPosition());
        validateOrderIndex(idx);
        while (true) {
            int curIdx = this.current.get();
            if (curIdx >= idx || this.current.compareAndSet(curIdx, idx))
                break;
        }
    }

    public Order first() {
        Order first = this.orders.get(1);
        checkValidOrders(first);
        return first;
    }

    public Order current() {
        Order currentOrder = orders.get(this.current.get());
        checkValidOrders(currentOrder);
        return currentOrder;
    }

    public double nextOrderPrice(Order order) {
        int idx = this.posToIdx.get(order.getPosition());
        BigDecimal followRate = this.followRates.get(idx);
        BigDecimal price = BigDecimal.valueOf(order.getPrice());
        BigDecimal rate = order.getPosSide() == PosSide.LongSide ? BigDecimal.ONE.subtract(followRate) : BigDecimal.ONE.add(followRate);
        return price.multiply(rate, DEFAULT_MATH_CONTEXT).doubleValue();
    }

    public List<Order> allOrders() {
        return new ArrayList<>(this.orders.values());
    }

    private void checkValidOrders(Order... orders) {
        for (Order order : orders)
            if (order.getPosSide() == null || order.getSide() == null)
                throw new IllegalStateException("set PosSide & Side before using orders");
    }

    private void validateOrderIndex(Integer idx) {
        if (idx == null || idx == 0 || idx > ORDER_MAX_ORDERS)
            throw new IllegalStateException("Illegal order idx: " + idx);
    }

    public static void main(String[] args) {
        MartinOrders instance = MartinOrders.instance();
        instance.setPosSide(1, PosSide.ShortSide);
        instance.allOrders().forEach(System.out::println);
        System.out.println(instance.followRates);

    }

}
