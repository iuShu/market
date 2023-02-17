package org.iushu.market.trade;

import org.iushu.market.config.OrderProperties;
import org.iushu.market.config.TradingProperties;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.iushu.market.trade.CalculateUtils.*;

public class MartinOrderUtils {

    public static int contractSize(int idx, OrderProperties properties) {
        int size = properties.getFirstContractSize();
        for (int i = 0; i < idx; i++)
            size = nextContractSize(size, properties);
        return size;
    }

    private static int orderIndex(int orderContractSize, OrderProperties properties) {
        int idx = 0, cs = properties.getFirstContractSize();
        for (int i = 0; i < properties.getMaxOrder() + 1; i++) {
            if (cs == orderContractSize)
                return idx;
            cs = nextContractSize(cs, properties);
            idx++;
        }
        throw new IllegalArgumentException("unexpected index of contract size " + orderContractSize);
    }

    public static int nextContractSize(int orderContractSize, OrderProperties properties) {
        return orderContractSize * properties.getContractIncrementRate();
    }

    public static int totalContractSize(int orderContractSize, OrderProperties properties) {
        int total = 0, cs = properties.getFirstContractSize();
        for (int i = 0; i <= orderIndex(orderContractSize, properties); i++) {
            if (cs == orderContractSize)
                return total + orderContractSize;
            total += cs;
            cs = nextContractSize(cs, properties);
        }
        throw new IllegalArgumentException("unexpected contract size " + orderContractSize);
    }

    private static double averagePrice(double firstPx, int orderContractSize, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderContractSize, properties);
        BigDecimal totalPx = ZERO;
        double orderPx = firstPx;
        for (int i = 0; i <= idx; i++) {
            int cs = contractSize(i, properties);
            totalPx = add(totalPx, mlt(decimal(orderPx), decimal(cs)));
            orderPx = nextOrderPrice(firstPx, cs, posSide, properties);
        }
        return doubleNum(div(totalPx, decimal(totalContractSize(orderContractSize, properties))));
    }

    private static double totalOpenFee(double firstPx, int orderContractSize, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderContractSize, properties);
        BigDecimal totalFee = ZERO;
        BigDecimal px = decimal(firstPx);
        for (int i = 0; i <= idx; i++) {
            int cs = contractSize(i, properties);
            BigDecimal fee = mlt(px, mlt(decimal(cs), decimal(properties.getFaceValue())));
            fee = mlt(fee, decimal(i == 0 ? properties.getMakerFeeRate() : properties.getTakerFeeRate()));
            totalFee = add(totalFee, fee);
            px = decimal(nextOrderPrice(firstPx, cs, posSide, properties));
        }
        return doubleNum(totalFee);
    }

    private static double closeFee(double firstPx, int orderContractSize, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderContractSize, properties);
        int cs = contractSize(idx, properties);
        int ttlCs = totalContractSize(cs, properties);
        BigDecimal px = decimal(takeProfitPrice(firstPx, cs, posSide, properties));
        BigDecimal fee = mlt(mlt(px, mlt(decimal(ttlCs), decimal(properties.getFaceValue()))), decimal(properties.getMakerFeeRate()));
        return doubleNum(fee);
    }

    private static double lossCloseFee(double firstPx, PosSide posSide, int lever, OrderProperties properties) {
        double lossPx = stopLossPrice(firstPx, posSide, properties);
        double avgPx = averagePrice(firstPx, lastContractSize(properties), posSide, properties);
        BigDecimal lv = decimal(lever);
        BigDecimal remainRate = posSide == PosSide.LongSide ? sub(avgPx, lossPx) : sub(lossPx, avgPx);
        remainRate = sub(ONE, mlt(div(remainRate, decimal(avgPx)), lv));
        BigDecimal totalContractSize = mlt(totalContractSize(lastContractSize(properties), properties), properties.getFaceValue());
        BigDecimal lastActualAvgVal = div(mlt(decimal(avgPx), totalContractSize), lv);
        return doubleNum(mlt(mlt(mlt(lastActualAvgVal, remainRate), lv), decimal(properties.getMakerFeeRate())));
    }

    public static int lastContractSize(OrderProperties properties) {
        return contractSize(properties.getMaxOrder() - 1, properties);
    }

    public static double nextOrderPrice(double firstPx, int orderContractSize, PosSide posSide, OrderProperties properties) {
        int nextCs = nextContractSize(orderContractSize, properties);
        int idx = orderIndex(nextCs, properties);
        double followRate = properties.getFollowRates().get(idx - 1);
        BigDecimal rate = posSide == PosSide.LongSide ? sub(ONE, decimal(followRate)) : add(ONE, decimal(followRate));
        return doubleNum(mlt(decimal(firstPx), rate));
    }

    public static double takeProfitPrice(double firstPx, int orderContractSize, PosSide posSide, OrderProperties properties) {
        int idx = orderIndex(orderContractSize, properties);
        double pullbackRate = properties.getPullbackRates().get(idx);
        BigDecimal rate = posSide == PosSide.LongSide ? add(ONE, decimal(pullbackRate)) : sub(ONE, decimal(pullbackRate));
        return doubleNum(mlt(rate, decimal(averagePrice(firstPx, orderContractSize, posSide, properties))));
    }

    public static double stopLossPrice(double firstPx, PosSide posSide, OrderProperties properties) {
        int lastCs = contractSize(properties.getMaxOrder() - 1, properties);
        return nextOrderPrice(firstPx, lastCs, posSide, properties);
    }

    public static double totalCost(double firstPx, int orderContractSize, PosSide posSide, int lever, OrderProperties properties) {
        int idx = orderIndex(orderContractSize, properties);
        BigDecimal totalValue = ZERO, px = decimal(firstPx);
        for (int i = 0; i <= idx; i++) {
            int cs = contractSize(i, properties);
            totalValue = add(totalValue, mlt(px, mlt(decimal(cs), decimal(properties.getFaceValue()))));
            px = decimal(nextOrderPrice(firstPx, cs, posSide, properties));
        }
        double openFee = totalOpenFee(firstPx, orderContractSize, posSide, properties);
        double closeFee = lossCloseFee(firstPx, posSide, lever, properties);
        return doubleNum(add(add(div(totalValue, decimal(lever)), decimal(openFee)), decimal(closeFee)));
    }

    public static double calcPnl(double firstPx, int orderContractSize, PosSide posSide, TradingProperties properties, double endPx) {
        double avgPx = averagePrice(firstPx, orderContractSize, posSide, properties.getOrder());
        BigDecimal avgPxDecimal = decimal(avgPx), leverDecimal = decimal(properties.getLever());
        BigDecimal pnlRate = posSide == PosSide.LongSide ? mlt(div(sub(endPx, avgPx), avgPxDecimal), leverDecimal)
                : mlt(div(sub(avgPx, endPx), avgPxDecimal), leverDecimal);
        BigDecimal faceValueDecimal = decimal(properties.getOrder().getFaceValue());
        BigDecimal avgValue = div(mlt(avgPx, totalContractSize(orderContractSize, properties.getOrder())), leverDecimal);
        return doubleNum(mlt(mlt(avgValue, faceValueDecimal), pnlRate));
    }

    public static void main(String[] args) {
//        List<Integer> contractSizes = Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128);
        List<Integer> contractSizes = Arrays.asList(1, 2, 4, 8, 16, 32);

        OrderProperties orderProperties = new OrderProperties();
        orderProperties.setContractIncrementRate(2);
        orderProperties.setFirstContractSize(1);
        orderProperties.setMaxOrder(contractSizes.size());
        orderProperties.setFaceValue(0.01);
        orderProperties.setMakerFeeRate(0.0005);
        orderProperties.setTakerFeeRate(0.0002);
        orderProperties.setFollowRates(Arrays.asList(0.006, 0.012, 0.018, 0.024, 0.030, 0.032));
        orderProperties.setPullbackRates(Arrays.asList(0.006, 0.006, 0.006, 0.006, 0.006, 0.003));

        int lever = 60;
        double price = 24904.8;

//        contractSizes.forEach(cs -> System.out.println(cs + " " + orderIndex(cs, orderProperties)));

//        for (int i = 0; i < contractSizes.size(); i++)
//            System.out.println(i + " " + contractSize(i, orderProperties));

//        contractSizes.forEach(cs -> System.out.println(cs + " " + nextOrderPrice(price, cs, PosSide.LongSide, orderProperties)));

//        contractSizes.forEach(cs -> System.out.println(cs + " " + totalContractSize(cs, orderProperties)));

//        contractSizes.forEach(cs -> System.out.println(cs + " " + averagePrice(price, cs, PosSide.LongSide, orderProperties)));

//        contractSizes.forEach(cs -> System.out.println(cs + " " + takeProfitPrice(price, cs, PosSide.LongSide, orderProperties)));

//        System.out.println(stopLossPrice(price, PosSide.LongSide, orderProperties));

//        contractSizes.forEach(cs -> System.out.println(cs + " " + totalOpenFee(price, cs, PosSide.LongSide, orderProperties)));

//        System.out.println(lossCloseFee(price, PosSide.LongSide, lever, orderProperties));

//        contractSizes.forEach(cs -> System.out.println(cs + " " + closeFee(price, cs, PosSide.LongSide, orderProperties)));

//        contractSizes.forEach(cs -> System.out.println(cs + " " + totalCost(price, cs, PosSide.LongSide, lever, orderProperties)));

//        for (int i = 0; i < orderProperties.getMaxOrder() - 1; i++) {
//            int cs = contractSize(i, orderProperties);
//            double nextOrderPrice = nextOrderPrice(price, cs, PosSide.LongSide, orderProperties);
//            System.out.println(nextContractSize(cs, orderProperties) + " " + nextOrderPrice);
//        }

    }

}
