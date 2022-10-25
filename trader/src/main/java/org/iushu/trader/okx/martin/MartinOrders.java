package org.iushu.trader.okx.martin;

import org.iushu.trader.base.Constants;
import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.Setting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.iushu.trader.base.Constants.*;
import static org.iushu.trader.okx.Setting.*;

public class MartinOrders {

    private final BigDecimal followRate;
    private final BigDecimal profitStepRate;
    private final int maxOrder;
    private final AtomicInteger batch = new AtomicInteger(1);
    private final AtomicInteger current = new AtomicInteger(0);
    private final Map<Integer, Order> orders;
    private final Map<Integer, Integer> posToOrders;
    private final AtomicBoolean resetting = new AtomicBoolean(false);

    private final BigDecimal ONE_THOUSAND = new BigDecimal("1000");
    private final BigDecimal ORDER_LEVER = new BigDecimal(Setting.ORDER_LEVER);
    private final MathContext DEFAULT_MATH_CONTEXT = new MathContext(10);

    private static final MartinOrders INSTANCE = new MartinOrders();

    private MartinOrders() {
        this.followRate = BigDecimal.valueOf(ORDER_FOLLOW_RATE);
        this.profitStepRate = BigDecimal.valueOf(ORDER_PROFIT_STEP_RATE);
        this.maxOrder = ORDER_MAX_ORDERS;
        this.orders = new ConcurrentHashMap<>(maxOrder);
        this.posToOrders = new ConcurrentHashMap<>(maxOrder);
        prepareOrders();
    }

    public static MartinOrders instance() {
        return INSTANCE;
    }

    private void prepareOrders() {
        Order first = new Order();
        first.setSide(SIDE_OPEN);
        first.setPosSide(POS_SIDE);
        first.setPosition(ORDER_START_POS);
        first.setOrderType(ORDER_TYPE_MARKET);
        orders.put(1, first);
        posToOrders.put(first.getPosition(), 1);
        for (int i = 2; i <= this.maxOrder; i++) {
            Order nextOrder = createNextOrder(orders.get(i - 1));
            orders.put(i, nextOrder);
            posToOrders.put(nextOrder.getPosition(), i);
        }
    }

    public Order first() {
        return orders.get(1);
    }

    public Order getOrder(int position) {
        Integer idx = this.posToOrders.get(position);
        return idx == null ? null : this.orders.get(idx);
    }

    public Collection<Order> allOrders() {
        return new ArrayList<>(this.orders.values());
    }

    public void setCurrent(Order order) {
        Integer idx = this.posToOrders.get(order.getPosition());
        validateIdx(idx);
        while (true) {
            int curIdx = this.current.get();
            if (curIdx >= idx || this.current.compareAndSet(curIdx, idx))
                break;
        }
    }

    public Order current() {
        return orders.get(current.get());
    }

    public int getBatch() {
        return this.batch.get();
    }

    private Order createNextOrder(Order order) {
        Order next = new Order();
        next.setSide(SIDE_SELL);
        next.setPosSide(POS_SIDE);
        next.setPrice(nextOrderPrice(order));
        next.setPosition(order.getPosition() * 2);
        next.setOrderType(ORDER_TYPE_LIMIT);
        return next;
    }

    public boolean validOrder(Order order) {
        return order != null &&
                (order.getOrderId() != null || Constants.ORDER_STATE_FILLED.equals(order.getState()));
    }

    public void calcOrdersPrice() {
        Order prev = first();
        for (int i = 2; i <= this.maxOrder; i++) {
            Order order = this.orders.get(i);
            order.setPrice(nextOrderPrice(prev));
            prev = order;
        }
    }

    public double takeProfitPrice(Order order) {
        Integer idx = this.posToOrders.get(order.getPosition());
        BigDecimal px = BigDecimal.valueOf(order.getPrice());
        BigDecimal tpRate = this.followRate.add(this.profitStepRate.multiply(new BigDecimal(String.valueOf(idx - 1))));
        tpRate = order.getPosSide() == PosSide.LongSide ? BigDecimal.ONE.add(tpRate) : BigDecimal.ONE.subtract(tpRate);
        return px.multiply(tpRate).doubleValue();
    }

    public double nextOrderPrice(Order order) {
        BigDecimal price = BigDecimal.valueOf(order.getPrice());
        return price.multiply(BigDecimal.ONE.add(this.followRate), DEFAULT_MATH_CONTEXT).doubleValue();
    }

    public double extraMarginBalance(Order order) {
        Integer idx = this.posToOrders.get(order.getPosition());
        BigDecimal value = new BigDecimal(order.getPosition()).divide(ONE_THOUSAND, DEFAULT_MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(order.getPrice())).divide(ORDER_LEVER, DEFAULT_MATH_CONTEXT);
        BigDecimal offset = new BigDecimal(ORDER_MAX_ORDERS - idx + 1);
        BigDecimal rate = this.followRate.multiply(ORDER_LEVER).multiply(offset).subtract(BigDecimal.ONE);
        double extra =  value.multiply(rate).doubleValue();
        return extra > 0 ? extra : 0;
    }

    private void validateIdx(Integer idx) {
        if (idx == null || idx == 0 || idx > ORDER_MAX_ORDERS)
            throw new IllegalStateException("Illegal order idx: " + idx);
    }

    public void reset() {
        if (!this.resetting.compareAndSet(false, true))
            return;
        this.batch.incrementAndGet();
        this.current.set(0);
        this.orders.clear();
        this.posToOrders.clear();
        prepareOrders();
        this.resetting.compareAndSet(true, false);
    }

    public double orderCost(Order order) {
        return BigDecimal.valueOf(order.getPrice())
                .divide(BigDecimal.valueOf(Setting.ORDER_LEVER), DEFAULT_MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(order.getPosition()).divide(ONE_THOUSAND, DEFAULT_MATH_CONTEXT)).doubleValue();
    }

    public double totalCost() {
        BigDecimal[] cost = new BigDecimal[]{BigDecimal.ZERO};
        this.allOrders().forEach(order -> cost[0] = cost[0].add(BigDecimal.valueOf(orderCost(order))));
        return cost[0].doubleValue();
    }

    public double openFee(Order order) {
        BigDecimal actualPos = BigDecimal.valueOf(order.getPosition()).divide(ONE_THOUSAND, DEFAULT_MATH_CONTEXT);
        return BigDecimal.valueOf(order.getPrice()).multiply(actualPos).multiply(BigDecimal.valueOf(ORDER_FEE_RATE)).doubleValue();
    }

    public double totalFee() {
        BigDecimal[] fee = new BigDecimal[]{BigDecimal.ZERO};
        Order[] last = new Order[1];
        int[] totalPos = new int[1];
        this.allOrders().forEach(order -> {
            fee[0] = fee[0].add(BigDecimal.valueOf(this.openFee(order)));
            if (last[0] == null || last[0].getPosition() < order.getPosition())
                last[0] = order;
            totalPos[0] += order.getPosition();
        });
        BigDecimal actualPos = BigDecimal.valueOf(totalPos[0]).divide(ONE_THOUSAND, DEFAULT_MATH_CONTEXT);
        return BigDecimal.valueOf(nextOrderPrice(last[0])).multiply(actualPos).multiply(BigDecimal.valueOf(ORDER_FEE_RATE)).doubleValue();
    }

    public void printOrders() {
        this.orders.forEach((k, v) -> System.out.println(k + " " + v));
    }

    public static void main(String[] args) {
        MartinOrders martinOrders = MartinOrders.instance();
        martinOrders.first().setPrice(20000);
        martinOrders.calcOrdersPrice();
        martinOrders.allOrders().forEach(o -> System.out.println(o.getPosition() + " ~ " + o.getPrice()
                + " | " + martinOrders.orderCost(o)
                + " + " + martinOrders.openFee(o)));
        double totalCost = martinOrders.totalCost();
        System.out.println(totalCost);
        double totalFee = martinOrders.totalFee();
        System.out.println(totalFee);
    }

}
