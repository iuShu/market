package org.iushu.market.trade;

import org.iushu.market.config.OrderProperties;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.iushu.market.trade.CalculateUtils.*;

public class MartinOrderUtils {

    private static int orderPos(int idx, OrderProperties properties) {
        int pos = properties.getPosStart();
        for (int i = 0; i < idx; i++)
            pos *= properties.getPosIncrementRate();
        return pos;
    }

    private static int orderIndex(int pos, OrderProperties properties) {
        int idx = 0, p = properties.getPosStart();
        for (int i = 0; i < properties.getMaxOrder() + 1; i++) {
            if (p == pos)
                return idx;
            p *= properties.getPosIncrementRate();
            idx++;
        }
        throw new IllegalArgumentException("unexpected index of pos " + pos);
    }

    private static int totalPosition(int pos, OrderProperties properties) {
        int total = 0, p = properties.getPosStart();
        for (int i = 0; i <= orderIndex(pos, properties); i++) {
            if (p == pos)
                return total + pos;
            total += p;
            p *= properties.getPosIncrementRate();
        }
        throw new IllegalArgumentException("unexpected pos " + pos);
    }

    private static double averagePrice(double firstPx, int orderPos, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderPos, properties);
        BigDecimal totalPx = ZERO;
        double orderPx = firstPx;
        for (int i = 0; i <= idx; i++) {
            int pos = orderPos(i, properties);
            totalPx = add(totalPx, mlt(decimal(orderPx), decimal(pos)));
            orderPx = nextOrderPrice(firstPx, pos, posSide, properties);
        }
        return doubleNum(div(totalPx, decimal(totalPosition(orderPos, properties))));
    }

    private static double totalOpenFee(double firstPx, int orderPos, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderPos, properties);
        BigDecimal totalFee = ZERO;
        BigDecimal px = decimal(firstPx);
        for (int i = 0; i <= idx; i++) {
            int pos = orderPos(i, properties);
            BigDecimal fee = mlt(px, div(decimal(pos), decimal(properties.getPosBase())));
            fee = mlt(fee, decimal(i == 0 ? properties.getMakerFeeRate() : properties.getTakerFeeRate()));
            totalFee = add(totalFee, fee);
            px = decimal(nextOrderPrice(firstPx, pos, posSide, properties));
        }
        return doubleNum(totalFee);
    }

    private static double totalCloseFee(double firstPx, int orderPos, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderPos, properties);
        BigDecimal totalFee = ZERO, px;
        for (int i = 0; i <= idx; i++) {
            int pos = orderPos(i, properties);
            int totalPos = totalPosition(pos, properties);
            px = decimal(takeProfitPrice(firstPx, pos, posSide, properties));
            BigDecimal fee = mlt(mlt(px, div(decimal(totalPos), decimal(properties.getPosBase()))), decimal(properties.getMakerFeeRate()));
            totalFee = add(totalFee, fee);
        }
        return doubleNum(totalFee);
    }

    public static int lastPos(OrderProperties properties) {
        return orderPos(properties.getMaxOrder() - 1, properties);
    }

    public static double nextOrderPrice(double firstPx, int orderPos, PosSide posSide, OrderProperties properties) {
        int nextPos = orderPos * properties.getPosIncrementRate();
        int idx = orderIndex(nextPos, properties);
        double followRate = properties.getFollowRates().get(idx - 1);
        BigDecimal rate = posSide == PosSide.LongSide ? sub(ONE, decimal(followRate)) : add(ONE, decimal(followRate));
        return doubleNum(mlt(decimal(firstPx), rate));
    }

    public static double takeProfitPrice(double firstPx, int orderPos, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderPos, properties);
        double pullbackRate = properties.getPullbackRates().get(idx);
        BigDecimal rate = posSide == PosSide.LongSide ? add(ONE, decimal(pullbackRate)) : sub(ONE, decimal(pullbackRate));
        return doubleNum(mlt(rate, decimal(averagePrice(firstPx, orderPos, posSide, properties))));
    }

    public static double stopLossPrice(double firstPx, PosSide posSide, OrderProperties properties) {
        int lastPos = orderPos(properties.getMaxOrder() - 1, properties);
        return nextOrderPrice(firstPx, lastPos, posSide, properties);
    }

    public static double totalCost(double firstPx, int orderPos, PosSide posSide, int lever, OrderProperties properties) {
        int idx = orderIndex(orderPos, properties);
        BigDecimal totalValue = ZERO, px = decimal(firstPx);
        for (int i = 0; i <= idx; i++) {
            int pos = orderPos(i, properties);
            totalValue = add(totalValue, mlt(px, div(decimal(pos), decimal(properties.getPosBase()))));
            px = decimal(nextOrderPrice(firstPx, pos, posSide, properties));
        }
        double openFee = totalOpenFee(firstPx, orderPos, posSide, properties);
        double closeFee = totalCloseFee(firstPx, orderPos, posSide, properties);
        return doubleNum(add(add(div(totalValue, decimal(lever)), decimal(openFee)), decimal(closeFee)));
    }

    public static void main(String[] args) {
        List<Integer> positions = Arrays.asList(10, 20, 40, 80, 160, 320, 640, 1280);

        OrderProperties orderProperties = new OrderProperties();
        orderProperties.setPosIncrementRate(2);
        orderProperties.setPosStart(10);
        orderProperties.setMaxOrder(8);
        orderProperties.setPosBase(1000);
        orderProperties.setMakerFeeRate(0.0005);
        orderProperties.setTakerFeeRate(0.0002);
        orderProperties.setFollowRates(Arrays.asList(0.006, 0.012, 0.018, 0.024, 0.03, 0.036, 0.042, 0.048));
        orderProperties.setPullbackRates(Arrays.asList(0.006, 0.006, 0.006, 0.006, 0.006, 0.006, 0.006, 0.006));

//        positions.forEach(p -> System.out.println(p + " " + orderIndex(p, orderProperties)));

//        for (int i = 0; i < 8; i++)
//            System.out.println(i + " " + orderPos(i, orderProperties));

//        positions.forEach(p -> System.out.println(p + " " + nextOrderPrice(16646.5, p, PosSide.LongSide, orderProperties)));

//        positions.forEach(p -> System.out.println(p + " " + totalPosition(p, orderProperties)));

//        positions.forEach(p -> System.out.println(p + " " + averagePrice(16646.5, p, PosSide.LongSide, orderProperties)));

//        positions.forEach(p -> System.out.println(p + " " + takeProfitPrice(16646.5, p, PosSide.LongSide, orderProperties)));

//        System.out.println(stopLossPrice(16646.5, PosSide.LongSide, orderProperties));

//        positions.forEach(p -> System.out.println(p + " " + totalOpenFee(16646.5, p, PosSide.LongSide, orderProperties)));

//        positions.forEach(p -> System.out.println(p + " " + totalCloseFee(16646.5, p, PosSide.LongSide, orderProperties)));

        positions.forEach(p -> System.out.println(p + " " + totalCost(16646.5, p, PosSide.LongSide, 80, orderProperties)));

    }

}
