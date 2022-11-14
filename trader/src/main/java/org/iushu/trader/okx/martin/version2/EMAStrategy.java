package org.iushu.trader.okx.martin.version2;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.PosSide;
import org.iushu.trader.okx.OkxHttpUtils;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.Setting;
import org.iushu.trader.okx.martin.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.iushu.trader.okx.OkxWsJsonClient.KEY_DATA;
import static org.iushu.trader.okx.Setting.*;

public class EMAStrategy implements Strategy<JSONObject>, OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EMAStrategy.class);

    private final List<JSONArray> repo = new CopyOnWriteArrayList<>();
    private static final int MAX_REPO_ELEMENTS = 200;

    private volatile double ema = 0.0;
    private JSONArray current = null;
    private volatile long displayInterval = 0L;
    private final Lock acceptLock = new ReentrantLock();
    private volatile long timestamp = 0L;

    public EMAStrategy() {
//        this.prepare();
    }

    @Override
    public JSONObject publicChannel() {
        return CHANNEL_CANDLE;
    }

    @Override
    public void consume(JSONObject message) {
        if (message.containsKey("op"))
            return;
        this.feed(message);
    }

    @Override
    public void prepare() {
        JSONArray candles = OkxHttpUtils.getCandleHistory(CANDLE_TYPE, MAX_REPO_ELEMENTS);
        if (candles.isEmpty()) {
            logger.error("prepare error, system exit.");
            System.exit(1);
        }

        List<JSONArray> list = candles.toList(JSONArray.class);
        Collections.reverse(list);
        this.repo.addAll(list);
        calcAndSetEMA();
        logger.info("prepared {} candle data", this.repo.size());
    }

    @Override
    public void feed(JSONObject message) {
        if (message == null || message.isEmpty())
            return;
        JSONArray data = message.getJSONArray(KEY_DATA);
        if (data == null || data.isEmpty())
            return;

        JSONArray array = data.getJSONArray(0);
        long ts = array.getLong(0);
        if (ts - this.timestamp == CANDLE_TYPE_MILLISECONDS) {
            acceptLock.lock();
            try {
                if (ts - this.timestamp != CANDLE_TYPE_MILLISECONDS)    // recheck
                    return;                                             // other thread has been added
                this.repo.add(this.current);
                this.timestamp = ts;
                this.current = array;
                this.ema = calcAndSetEMAByPrevious(array.getDouble(4));
            } finally {
                acceptLock.unlock();
            }
            if (this.repo.size() >= MAX_REPO_ELEMENTS)
                this.repo.remove(0);
//            logger.info("accept candle data {} {}", this.repo.size(), this.current);
            return;
        }
        else if (this.timestamp != 0 && ts != this.timestamp) {
            logger.warn("current {} but recv {}", this.timestamp, ts);
        }
        else if (this.timestamp == 0 && !this.repo.isEmpty()) {
            JSONArray last = this.repo.get(this.repo.size() - 1);
            Long lastTs = last.getLong(0);
            if (ts == lastTs) {
                this.repo.remove(this.repo.size() - 1);     // remove latest invalid data
            }
            else if (ts - lastTs != CANDLE_TYPE_MILLISECONDS) {
                logger.warn("discard legacy data at {} before {}", lastTs, ts);
                this.repo.clear();
            }
        }
        this.timestamp = ts;
        this.current = array;
    }

    @Override
    public boolean satisfy(JSONObject message) {
        return false;
    }

    @Override
    public PosSide decideSide(JSONObject message) {
        if (this.ema == 0) {
            logger.warn("EMA not initialized out of expected");
            return null;
        }

        Double px = message.getDouble("last");
        double currentEMA = calcAndSetEMAByPrevious(px);
        for (PosSide posSide : PosSide.values()) {
            if (posSide.isProfit(currentEMA, px))
                return posSide;
        }
        return null;
    }

    private double calcAndSetEMA() {
        BigDecimal period = new BigDecimal(STRATEGY_EMA_TYPE);
        BigDecimal[] ema = {null};
        this.repo.forEach(each -> {
            BigDecimal closePrice = each.getBigDecimal(4);
            if (ema[0] == null)
                ema[0] = closePrice;
            else {
                BigDecimal left = new BigDecimal("2").multiply(closePrice).divide(period.add(BigDecimal.ONE), BigDecimal.ROUND_HALF_UP);
                BigDecimal right = period.subtract(BigDecimal.ONE).multiply(ema[0]).divide(period.add(BigDecimal.ONE), BigDecimal.ROUND_HALF_UP);
                ema[0] = left.add(right);
            }
            System.out.println(ema[0]);
        });
        this.ema = ema[0].setScale(4, RoundingMode.HALF_UP).doubleValue();
        return this.ema;
    }

    private double calcAndSetEMAByPrevious(double price) {
        BigDecimal period = new BigDecimal(STRATEGY_EMA_TYPE);
        BigDecimal px = BigDecimal.valueOf(price);
        BigDecimal previous = BigDecimal.valueOf(this.ema);
        return new BigDecimal("2").multiply(px.subtract(previous)).divide(BigDecimal.ONE.add(period), BigDecimal.ROUND_HALF_UP)
                .add(previous).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private void debugStrategyCheck(double ma, double px) {
        long now = System.currentTimeMillis();
        if (this.displayInterval < now) {
            logger.debug("strategy ma={} px={}", ma, px);
            this.displayInterval = now + Setting.CANDLE_TYPE_MILLISECONDS;
        }
    }

    public static void main(String[] args) {
//        String dataSerial = "7.8355, 7.2712, 7.4882, 8.7208, 9.6118, 9.6908, 9.4246, 8.9740, 9.1889, 8.1940, 7.9424, 7.8070, 7.8391, 7.9433, 7.2354, 7.3611, 7.3259, 7.7373, 7.8029, 7.0959, 6.1995, 4.2682, 5.2575, 4.7043, 4.6616, 4.2853";
        String dataSerial = "16685.7, 16773.1, 16651.2, 16541.2, 16584.8, 16660.0, 16609.0, 16671.0, 16634.7, 16579.7, 16594.6, 16561.8, 16538.6, 16570.1, 16496.9, 16394.6, 16448.3, 16336.9";
        String[] split = dataSerial.split(", ");
        List<JSONArray> list = new ArrayList<>();
        for (String each : split)
            list.add(JSONArray.of(0, 0, 0, 0, each));

        EMAStrategy strategy = new EMAStrategy();

//        strategy.repo = list;
//        double ema = strategy.calcAndSetEMA();
//        System.out.println(ema);

        strategy.ema = 16867.2;
        for (JSONArray array : list) {
            double ema = strategy.calcAndSetEMAByPrevious(array.getDouble(4));
            System.out.println(ema);
        }
    }

}
