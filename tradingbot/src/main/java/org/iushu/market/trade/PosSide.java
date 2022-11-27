package org.iushu.market.trade;

import org.iushu.market.Constants;

public enum PosSide {

    LongSide(Constants.POS_SIDE_LONG, Constants.SIDE_BUY, Constants.SIDE_SELL),

    ShortSide(Constants.POS_SIDE_SHORT, Constants.SIDE_SELL, Constants.SIDE_BUY),

    ;

    String name;
    String openSide;
    String closeSide;

    PosSide(String name, String openSide, String closeSide) {
        this.name = name;
        this.openSide = openSide;
        this.closeSide = closeSide;
    }

    public String getName() {
        return this.name;
    }

    public static PosSide of(String name) {
        for (PosSide posSide : values())
            if (posSide.getName().equals(name))
                return posSide;
        return null;
    }

    public boolean isProfit(double px, double lastPx) {
        return this == LongSide ? px < lastPx : px > lastPx;
    }

    public boolean isLoss(double px, double lastPx) {
        return !isProfit(px, lastPx);
    }

    public String openSide() {
        return openSide;
    }

    public String closeSide() {
        return closeSide;
    }

    public static void main(String[] args) {
        double px = 19299.2;
        double ma = 20000.0;
        System.out.println(PosSide.ShortSide.isLoss(ma, px));
    }

}
