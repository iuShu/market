package org.iushu.trader.okx.martin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.iushu.trader.base.Constants;
import org.iushu.trader.okx.OkxHttpUtils;
import org.iushu.trader.okx.OkxMessageConsumer;
import org.iushu.trader.okx.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.iushu.trader.okx.OkxWsJsonClient.KEY_DATA;
import static org.iushu.trader.okx.Setting.*;

public class MAStrategy implements Strategy<JSONObject>, OkxMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MAStrategy.class);

    private final List<JSONArray> repo = new CopyOnWriteArrayList<>();
    private static final int MAX_REPO_ELEMENTS = STRATEGY_MA_TYPE * 2;

    private final Lock acceptLock = new ReentrantLock();
    private volatile long timestamp = 0L;
    private JSONArray current = null;

    public MAStrategy() {
        this.prepare();
    }

    @Override
    public JSONObject publicChannel() {
        return CHANNEL_CANDLE;
    }

    @Override
    public void consume(JSONObject message) {
        this.feed(message);
    }

    @Override
    public void prepare() {
        JSONArray candles = OkxHttpUtils.getCandleHistory(CANDLE_TYPE, STRATEGY_MA_TYPE + 1);
        if (candles.isEmpty()) {
            logger.warn("prepare error, waiting ws client for feeding data");
            System.exit(1);
        }

        List<JSONArray> list = candles.toList(JSONArray.class);
        Collections.reverse(list);
        this.repo.addAll(list);
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
            } finally {
                acceptLock.unlock();
            }
            if (this.repo.size() >= MAX_REPO_ELEMENTS)
                this.repo.remove(0);
            logger.info("accept candle data {} {}", this.repo.size(), this.current);
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
        if (this.repo.size() < STRATEGY_MA_TYPE)
            return false;
        Double px = message.getDouble("last");

        int size = this.repo.size();
        List<JSONArray> subList = this.repo.subList(size - STRATEGY_MA_TYPE, size);
        final BigDecimal[] total = {BigDecimal.ZERO};
        subList.forEach(each -> total[0] = total[0].add(each.getBigDecimal(4)));
        double ma = total[0].divide(new BigDecimal(String.valueOf(STRATEGY_MA_TYPE)), BigDecimal.ROUND_HALF_UP).doubleValue();
        return POS_SIDE.isLoss(ma, px);
    }

}
