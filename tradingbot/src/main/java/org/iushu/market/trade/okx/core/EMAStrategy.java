package org.iushu.market.trade.okx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.market.trade.PosSide;
import org.iushu.market.trade.okx.OkxConstants;
import org.iushu.market.trade.okx.OkxRestTemplate;
import org.iushu.market.trade.okx.Strategy;
import org.iushu.market.trade.okx.config.OkxComponent;
import org.iushu.market.trade.okx.config.SubscribeChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ONE;
import static org.iushu.market.trade.CalculateUtils.*;
import static org.iushu.market.trade.okx.OkxConstants.CANDLE_PERIOD_MILLISECONDS;

@OkxComponent
public class EMAStrategy implements Strategy<Double> {

    private static final Logger logger = LoggerFactory.getLogger(EMAStrategy.class);
    private static final int PERIOD = 12;
    private static final int MAX_REPO_ELEMENTS = 200;

    private final OkxRestTemplate restTemplate;
    private final List<JSONArray> repository = new CopyOnWriteArrayList<>();
    private volatile double EMAValue = 0.0;
    private volatile long displayInterval = 0L;
    private final AtomicBoolean preparing = new AtomicBoolean(false);
    private final AtomicBoolean accepting = new AtomicBoolean(false);
    private volatile long timestamp = 0L;
    private JSONArray current = null;

    public EMAStrategy(OkxRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void prepare() {
        JSONArray history = restTemplate.getCandleHistory(OkxConstants.CANDLE_BAR, MAX_REPO_ELEMENTS);
        if (history.isEmpty()) {
            logger.error("strategy preparing candle data error, using tickers data");
            return;
        }

        List<JSONArray> list = history.stream().map(each -> new JSONArray((Collection) each)).collect(Collectors.toList());
        Collections.reverse(list);
        repository.addAll(list);
        calculateAndSaveValue();
        logger.info("prepared {} candle data and got EMA12 {}", repository.size(), EMAValue);
    }

    @SubscribeChannel(channel = OkxConstants.CHANNEL_CANDLE)
    public void feedTradingData(JSONObject message) {
        if (repository.isEmpty()) {
            if (!preparing.compareAndSet(false, true))
                return;
            prepare();
            preparing.compareAndSet(true, false);
        }

        JSONArray data = message.getJSONArray("data");
        JSONArray array = data.getJSONArray(0);
        long ts = array.getLong(0);
        if (ts - timestamp == CANDLE_PERIOD_MILLISECONDS) {
            acceptTicker(ts, array);
            return;
        }
        else if (timestamp == 0 && !repository.isEmpty()) {
            JSONArray last = repository.get(repository.size() - 1);
            Long lastTs = last.getLong(0);
            if (ts == lastTs) {
                repository.remove(repository.size() - 1);     // remove latest invalid data
            }
            else if (ts - lastTs != CANDLE_PERIOD_MILLISECONDS) {
                logger.warn("discard legacy data at {} before {}", lastTs, ts);
                repository.clear();
            }
        }
        timestamp = ts;
        current = array;
    }

    @Override
    public PosSide trend(Double price) {
        if (EMAValue == 0) {
//            logger.warn("unexpected EMA value");
            return null;
        }

        double currentEMA = calculateAndSaveValue(price);
        for (PosSide posSide : PosSide.values()) {
            if (posSide.isProfit(currentEMA, price))
                return posSide;
        }
        return null;
    }

    private void acceptTicker(long ts, JSONArray array) {
        if (!accepting.compareAndSet(false, true) || ts - timestamp != CANDLE_PERIOD_MILLISECONDS)
            return;

        try {
            repository.add(current);
            timestamp = ts;
            current = array;
            EMAValue = calculateAndSaveValue(array.getDouble(4));
            if (repository.size() >= MAX_REPO_ELEMENTS)
                repository.remove(0);
            logger.debug("accept candle data {} {}", repository.size(), this.current);
        } finally {
            accepting.compareAndSet(true, false);
        }
    }

    private void calculateAndSaveValue() {
        BigDecimal EMA = null;
        BigDecimal period = decimal(PERIOD);
        for (JSONArray each : repository) {
            BigDecimal closePrice = each.getBigDecimal(4);
            EMA = EMA == null ? closePrice : EMA;
            BigDecimal left = div(mlt(decimal(2.0), closePrice), add(period, ONE));
            EMA = add(left, div(mlt(sub(period, ONE), EMA), add(period, ONE)));
        }
        EMAValue = EMA == null ? 0.0 : doubleNum(EMA);
    }

    private double calculateAndSaveValue(double price) {
        BigDecimal period = decimal(PERIOD);
        BigDecimal px = decimal(price);
        BigDecimal previous = decimal(EMAValue);
        return doubleNum(add(div(mlt(decimal(2.0), sub(px, previous)), add(ONE, period)), previous));
    }

    private void debugStrategyCheck(double ma, double px) {
        long now = System.currentTimeMillis();
        if (displayInterval < now) {
            logger.debug("strategy ma={} px={}", ma, px);
            displayInterval = now + CANDLE_PERIOD_MILLISECONDS;
        }
    }

}
