package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxConstants;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static org.iushu.market.trade.CalculateUtils.*;

@Primary
@OkxComponent
public class EMAStatisticStrategy extends EMAStrategy {

    private static final Logger logger = LoggerFactory.getLogger(EMAStatisticStrategy.class);

    public static final int RANGE = 30;
    private final LinkedList<PosSide> trends = new LinkedList<>();

    public EMAStatisticStrategy(OkxRestTemplate restTemplate) {
        super(restTemplate);
    }

    @SubscribeChannel(channel = OkxConstants.CHANNEL_CANDLE)
    public void feedTradingData(JSONObject message) {
        super.feedTradingData(message);
    }

    @Override
    public PosSide trend(Double price) {
        if (trends.size() < RANGE) {
            logger.warn("not enough trends");
            return null;
        }

        List<PosSide> temp = new ArrayList<>(trends.subList(0, RANGE));
        double currentEma = calculateByPrevious(price);
        temp.add(0, determine(currentEma, price));
        long longCount = 0, shortCount = 0;
        for (PosSide posSide : temp) {
            if (posSide == PosSide.LongSide)
                longCount++;
            else
                shortCount++;
        }
        logger.info("trend statistics long={} short={}", longCount, shortCount);
        return longCount > shortCount ? PosSide.LongSide : PosSide.ShortSide;
    }

    private void addTrend(double ema, double price) {
        trends.offerFirst(determine(ema, price));
        if (trends.size() >= RANGE * 2)
            trends.pollLast();
    }

    private PosSide determine(double ema, double price) {
        return ema > price ? PosSide.ShortSide : PosSide.LongSide;
    }

    @Override
    protected void calculateByHistory() {
        BigDecimal EMA = null;
        BigDecimal period = decimal(PERIOD);
        for (JSONArray each : repository) {
            BigDecimal closePrice = each.getBigDecimal(4);
            EMA = EMA == null ? closePrice : EMA;
            BigDecimal left = div(mlt(decimal(2.0), closePrice), add(period, ONE));
            EMA = add(left, div(mlt(sub(period, ONE), EMA), add(period, ONE)));
            addTrend(doubleNum(EMA), doubleNum(closePrice));
        }
    }

    @Override
    protected double calculateByPrevious(double price) {
        double ema = super.calculateByPrevious(price);
        addTrend(ema, price);
        return ema;
    }

}
