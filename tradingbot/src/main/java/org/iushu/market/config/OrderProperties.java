package org.iushu.market.config;

import java.util.List;

public class OrderProperties {

    private int posBase;
    private int posStart;
    private int posIncrementRate;
    private int maxOrder;
    private List<Double> followRates;
    private List<Double> pullbackRates;
    private double makerFeeRate;
    private double takerFeeRate;

    public int getPosBase() {
        return posBase;
    }

    public void setPosBase(int posBase) {
        this.posBase = posBase;
    }

    public int getPosStart() {
        return posStart;
    }

    public void setPosStart(int posStart) {
        this.posStart = posStart;
    }

    public int getPosIncrementRate() {
        return posIncrementRate;
    }

    public void setPosIncrementRate(int posIncrementRate) {
        this.posIncrementRate = posIncrementRate;
    }

    public int getMaxOrder() {
        return maxOrder;
    }

    public void setMaxOrder(int maxOrder) {
        this.maxOrder = maxOrder;
    }

    public List<Double> getFollowRates() {
        return followRates;
    }

    public void setFollowRates(List<Double> followRates) {
        this.followRates = followRates;
    }

    public List<Double> getPullbackRates() {
        return pullbackRates;
    }

    public void setPullbackRates(List<Double> pullbackRates) {
        this.pullbackRates = pullbackRates;
    }

    public double getMakerFeeRate() {
        return makerFeeRate;
    }

    public void setMakerFeeRate(double makerFeeRate) {
        this.makerFeeRate = makerFeeRate;
    }

    public double getTakerFeeRate() {
        return takerFeeRate;
    }

    public void setTakerFeeRate(double takerFeeRate) {
        this.takerFeeRate = takerFeeRate;
    }

}
