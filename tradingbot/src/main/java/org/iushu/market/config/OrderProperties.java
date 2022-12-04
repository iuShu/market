package org.iushu.market.config;

import java.util.List;

public class OrderProperties {

    private double faceValue;
    private int firstContractSize;
    private int contractIncrementRate;
    private int maxOrder;
    private double extraMargin;
    private List<Double> followRates;
    private List<Double> pullbackRates;
    private double makerFeeRate;
    private double takerFeeRate;

    public double getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(double faceValue) {
        this.faceValue = faceValue;
    }

    public int getFirstContractSize() {
        return firstContractSize;
    }

    public void setFirstContractSize(int firstContractSize) {
        this.firstContractSize = firstContractSize;
    }

    public int getContractIncrementRate() {
        return contractIncrementRate;
    }

    public void setContractIncrementRate(int contractIncrementRate) {
        this.contractIncrementRate = contractIncrementRate;
    }

    public int getMaxOrder() {
        return maxOrder;
    }

    public void setMaxOrder(int maxOrder) {
        this.maxOrder = maxOrder;
    }

    public double getExtraMargin() {
        return extraMargin;
    }

    public void setExtraMargin(double extraMargin) {
        this.extraMargin = extraMargin;
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
