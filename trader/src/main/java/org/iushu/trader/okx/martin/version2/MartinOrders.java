package org.iushu.trader.okx.martin.version2;

import org.iushu.trader.base.Constants;
import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.okx.martin.Order;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.iushu.trader.base.Constants.*;
import static org.iushu.trader.okx.Setting.*;

public class MartinOrders {

    private final int maxOrder;
    private final Map<Integer, BigDecimal> followRates = new HashMap<>(ORDER_MAX_ORDERS);
    private final Map<Integer, BigDecimal> marginRates = new HashMap<>(ORDER_MAX_ORDERS);
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

        Double[] followRates = {0.02, 0.04, 0.07, 0.14};
        Double[] marginRates = {0.0, 0.03, 0.05, 0.09};
        for (int i = 1; i <= this.maxOrder; i++) {
            this.followRates.put(i, BigDecimal.valueOf(followRates[i-1]));
            this.marginRates.put(i, BigDecimal.valueOf(marginRates[i-1]));
        }
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

    public void reset() {
        if (!this.resetting.compareAndSet(false, true))
            return;
        this.batch.incrementAndGet();
        this.current.set(0);
        this.orders.clear();
        this.posToIdx.clear();
        prepareOrders();
        this.resetting.compareAndSet(true, false);
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
        return this.orders.get(1);
    }

    public boolean isLastOrder(Order order) {
        return this.maxOrder == this.posToIdx.get(order.getPosition());
    }

    public Order current() {
        Order currentOrder = orders.get(this.current.get());
        checkValidOrders(currentOrder);
        return currentOrder;
    }

    public void calcOrdersPrice() {
        for (int i = 2; i <= this.maxOrder; i++) {  // start from no.2 order
            Order order = this.orders.get(i);
            order.setPrice(nextOrderPrice(this.orders.get(i - 1)));
        }
    }

    public double nextOrderPrice(Order order) {
        int idx = this.posToIdx.get(order.getPosition());
        BigDecimal followRate = this.followRates.get(idx);
        BigDecimal price = BigDecimal.valueOf(first().getPrice());
        BigDecimal rate = order.getPosSide() == PosSide.LongSide ? BigDecimal.ONE.subtract(followRate) : BigDecimal.ONE.add(followRate);
        return price.multiply(rate, DEFAULT_MATH_CONTEXT).doubleValue();
    }

    public double takeProfitPrice(Order order) {
        BigDecimal px = BigDecimal.valueOf(averageValue(order, false));
        BigDecimal tpRate = BigDecimal.valueOf(ORDER_TAKE_PROFIT_RATE).divide(ORDER_LEVER, 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal rate = order.getPosSide() == PosSide.LongSide ? BigDecimal.ONE.add(tpRate) : BigDecimal.ONE.subtract(tpRate);
        return px.multiply(rate).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private double averageValue(Order order, boolean value) {
        Integer idx = this.posToIdx.get(order.getPosition());
        BigDecimal totalPosition = BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < idx; i++) {
            Order o = this.orders.get(i + 1);
            BigDecimal pos = new BigDecimal(o.getPosition());
            sum = sum.add(BigDecimal.valueOf(o.getPrice()).multiply(pos));
            totalPosition = totalPosition.add(pos);
        }
        sum = sum.divide(totalPosition, 4, RoundingMode.HALF_UP);
        return value ? sum.multiply(totalPosition, DEFAULT_MATH_CONTEXT).divide(ONE_THOUSAND, 4, RoundingMode.HALF_UP).divide(ORDER_LEVER, 4, RoundingMode.HALF_UP).doubleValue()
                : sum.doubleValue();
    }

    public double totalExtraMargin() {
        BigDecimal margin = BigDecimal.ZERO;
        for (int i = 1; i <= this.maxOrder; i++) {
            Order order = this.orders.get(i);
            BigDecimal value = BigDecimal.valueOf(averageValue(order, true));
            margin = margin.add(value.multiply(this.marginRates.get(i)).multiply(ORDER_LEVER));
        }
        return margin.setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    public double totalCost(double price) {
        long count = this.allOrders().stream().filter(o -> o.getPosSide() != null).count();
        if (count != 0 && this.resetting.get())
            throw new IllegalStateException("total-cost calculation is not permitted currently");

        int rawBatch = this.batch.get();
        this.setPosSide(this.batch.get(), PosSide.ShortSide);
        this.first().setPrice(price);
        this.calcOrdersPrice();
        BigDecimal[] shortCost = {BigDecimal.ZERO};
        this.allOrders().forEach(o -> {
            BigDecimal val = BigDecimal.valueOf(o.getPrice()).multiply(new BigDecimal(o.getPosition()));
            shortCost[0] = shortCost[0].add(val.divide(ONE_THOUSAND, 4, RoundingMode.HALF_UP).divide(ORDER_LEVER, 4, RoundingMode.HALF_UP));
        });
        shortCost[0] = shortCost[0].add(BigDecimal.valueOf(this.totalExtraMargin()));

        this.setPosSide(this.batch.get(), PosSide.LongSide);
        this.first().setPrice(price);
        this.calcOrdersPrice();
        BigDecimal[] longCost = {BigDecimal.ZERO};
        this.allOrders().forEach(o -> {
            BigDecimal val = BigDecimal.valueOf(o.getPrice()).multiply(new BigDecimal(o.getPosition()));
            longCost[0] = longCost[0].add(val.divide(ONE_THOUSAND, 4, RoundingMode.HALF_UP).divide(ORDER_LEVER, 4, RoundingMode.HALF_UP));
        });
        longCost[0] = longCost[0].add(BigDecimal.valueOf(this.totalExtraMargin()));

        this.reset();
        if (!this.batch.compareAndSet(rawBatch - 1, rawBatch))
            throw new IllegalStateException("unexpected operation during total-cost calculation");
        return Math.max(shortCost[0].setScale(4, RoundingMode.HALF_UP).doubleValue(),
                longCost[0].setScale(4, RoundingMode.HALF_UP).doubleValue());
    }

    public int getBatch() {
        return this.batch.get();
    }

    public Order getOrder(int position) {
        Integer idx = this.posToIdx.get(position);
        Order order = this.orders.get(idx);
        checkValidOrders(order);
        return order;
    }

    public String openSide() {
        return first().getPosSide() == PosSide.LongSide ? SIDE_BUY : SIDE_SELL;
    }

    public String closeSide() {
        return first().getPosSide() == PosSide.LongSide ? SIDE_SELL : SIDE_BUY;
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

    public boolean validOrder(Order order) {
        return order != null && order.getPosSide() != null && order.getSide() != null
                && (order.getOrderId() != null || Constants.ORDER_STATE_FILLED.equals(order.getState()));
    }

    public static void main(String[] args) {
        MartinOrders instance = MartinOrders.instance();
        instance.setPosSide(1, PosSide.LongSide);
        instance.allOrders().forEach(System.out::println);
//        instance.allOrders().forEach(o -> System.out.println(instance.isLastOrder(o)));
        System.out.println(instance.followRates);
        System.out.println(instance.marginRates);
        instance.first().setPrice(20000.0);
        instance.calcOrdersPrice();
        instance.allOrders().forEach(System.out::println);
        System.out.println("stop price: " + instance.nextOrderPrice(instance.getOrder(80)));
        instance.allOrders().forEach(o -> System.out.println("avg price: " + instance.averageValue(o, false)));
        instance.allOrders().forEach(o -> System.out.println("avg value: " + instance.averageValue(o, true)));
        instance.allOrders().forEach(o -> System.out.println("profit price: " + instance.takeProfitPrice(o)));
        System.out.println("margin: " + instance.totalExtraMargin());
    }

}
