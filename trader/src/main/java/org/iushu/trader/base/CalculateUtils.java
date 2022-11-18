package org.iushu.trader.base;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CalculateUtils {

    public static final int SCALE = 6;

    public static BigDecimal decimal(double d) {
        return new BigDecimal(Double.toString(d));
    }

    public static BigDecimal add(double d1, double d2) {
        return add(decimal(d1), decimal(d2));
    }

    public static BigDecimal add(BigDecimal d1, BigDecimal d2) {
        return d1.add(d2);
    }

    public static BigDecimal sub(double d1, double d2) {
        return sub(decimal(d1), decimal(d2));
    }

    public static BigDecimal sub(BigDecimal d1, BigDecimal d2) {
        return d1.subtract(d2);
    }

    public static BigDecimal mlt(double d1, double d2) {
        return mlt(decimal(d1), decimal(d2));
    }

    public static BigDecimal mlt(BigDecimal d1, BigDecimal d2) {
        return d1.multiply(d2);
    }

    public static BigDecimal div(double d1, double d2) {
        return div(decimal(d1), decimal(d2));
    }

    public static BigDecimal div(BigDecimal d1, BigDecimal d2) {
        return d1.divide(d2, SCALE, RoundingMode.HALF_UP);
    }

    public static double doubleNum(BigDecimal decimal) {
        return decimal.setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    public static void main(String[] args) {

    }

}
