package org.iushu.trader.base;

public enum PosSide {

    LongSide(Constants.POS_SIDE_LONG),

    ShortSide(Constants.POS_SIDE_SHORT),

    ;

    String name;

    PosSide(String name) {
        this.name = name;
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

    public static void main(String[] args) {
        double px = 19299.2;
        double ma = 20000.0;
        System.out.println(PosSide.ShortSide.isLoss(ma, px));
    }

}
