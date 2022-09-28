package org.iushu.trader.okx.martin;

import org.iushu.trader.okx.Setting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.iushu.trader.base.Constants.*;
import static org.iushu.trader.okx.Setting.*;

public class MartinOrders {

    private BigDecimal followRate;
    private BigDecimal profitStepRate;
    private int maxOrder;
    private AtomicInteger current = new AtomicInteger(0);
    private Map<Integer, Order> orders;
//    private Map<Long, Integer> midToOrders;
    private Map<String, Integer> idToOrders;
    private Map<Integer, Integer> posToOrders;

    private final BigDecimal ONE_THOUSAND = new BigDecimal("1000");
    private final BigDecimal ORDER_LEVER = new BigDecimal(Setting.ORDER_LEVER);
    private final MathContext DEFAULT_MATH_CONTEXT = new MathContext(10);

    private static final MartinOrders INSTANCE = new MartinOrders();

    private MartinOrders() {
        this.followRate = BigDecimal.valueOf(ORDER_FOLLOW_RATE);
        this.profitStepRate = BigDecimal.valueOf(ORDER_PROFIT_STEP_RATE);
        this.maxOrder = ORDER_MAX_ORDERS;
        this.orders = new HashMap<>(maxOrder);
        this.idToOrders = new HashMap<>(maxOrder);
        this.posToOrders = new HashMap<>(maxOrder);
        prepareOrders();
    }

    public static MartinOrders instance() {
        return INSTANCE;
    }

    public void prepareOrders() {
        Order first = new Order();
        first.setSide(SIDE_SELL);
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

    public void setOrder(String orderId, int position) {
        Integer idx = this.posToOrders.get(position);
        validateIdx(idx);
        this.idToOrders.put(orderId, idx);
    }

    public Order getOrder(int position) {
        return this.orders.get(this.posToOrders.get(position));
    }

    public Order getOrder(String orderId) {
        return this.orders.get(this.idToOrders.get(orderId));
    }

    public Collection<Order> allOrders() {
        return this.orders.values();
    }

    public void setCurrent(Order order) {
        Integer idx = this.idToOrders.get(order.getOrderId());
        validateIdx(idx);
        this.current.compareAndSet(idx - 1, idx);
    }

    public Order current() {
        return orders.get(current.get());
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

    public void calcOrdersPrice() {
        Order prev = first();
        for (int i = 2; i <= this.maxOrder; i++) {
            Order order = this.orders.get(i);
            order.setPrice(nextOrderPrice(prev));
            prev = order;
        }
    }

    public double takeProfitPrice(Order order) {
        Integer idx = this.idToOrders.get(order.getOrderId());
        return this.followRate.add(this.profitStepRate.multiply(new BigDecimal(String.valueOf(idx - 1)))).doubleValue();
    }

    private double nextOrderPrice(Order order) {
        BigDecimal price = BigDecimal.valueOf(order.getPrice());
        return price.multiply(BigDecimal.ONE.add(this.followRate), DEFAULT_MATH_CONTEXT).doubleValue();
    }

    public double extraMarginBalance(Order order) {
        Integer idx = this.idToOrders.get(order.getOrderId());
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
        this.current.set(0);
        this.orders.clear();
        this.idToOrders.clear();
        this.posToOrders.clear();
        prepareOrders();
    }

    public static void main(String[] args) {
        MartinOrders martinOrders = MartinOrders.instance();
        martinOrders.first().setPrice(20000);
        martinOrders.calcOrdersPrice();
        martinOrders.allOrders().forEach(o -> System.out.println(o.getPosition() + " ~ " + o.getPrice()));
    }

}
