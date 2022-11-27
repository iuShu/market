package org.iushu.market.trade.okx;

import org.iushu.market.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface OkxConstants {

    int SUCCESS = 0;
    int CANDLE_PERIOD_MILLISECONDS = 60000;
    String CANDLE_BAR = Constants.CANDLE_TYPE_1M;

    String OP_ORDER = "order";

    String EVENT_LOGIN = "login";
    String EVENT_SUBSCRIBE = "subscribe";
    String EVENT_ERROR = "error";

    String CHANNEL_TICKERS = "tickers";
    String CHANNEL_ORDERS = "orders";
    String CHANNEL_CANDLE = "candle" + CANDLE_BAR;

    Set<String> PRIVATE_CHANNELS = new HashSet<>(Arrays.asList("orders", "balance"));

    String GET_CANDLE_HISTORY = "/api/v5/market/candles";
    String GET_LEVER = "/api/v5/account/leverage-info";
    String SET_LEVER = "/api/v5/account/set-leverage";
    String GET_BALANCE = "/api/v5/account/balance";

    String CANCEL_ALGO = "/api/v5/trade/cancel-algos";

}
