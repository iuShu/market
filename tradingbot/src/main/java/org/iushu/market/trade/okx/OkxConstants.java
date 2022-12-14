package org.iushu.market.trade.okx;

import org.iushu.market.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface OkxConstants {

    int SUCCESS = 0;
    int CANDLE_PERIOD_MILLISECONDS = 60000;
    String CANDLE_BAR = Constants.CANDLE_TYPE_1M;

    String OP_LOGIN = "login";
    String OP_ORDER = "order";
    String OP_SUBSCRIBE = "subscribe";
    String OP_BATCH_ORDERS = "batch-orders";
    String OP_BATCH_CANCEL_ORDERS = "batch-cancel-orders";

    String EVENT_LOGIN = OP_LOGIN;
    String EVENT_SUBSCRIBE = OP_SUBSCRIBE;
    String EVENT_BATCH_ORDERS = OP_BATCH_ORDERS;
    String EVENT_BATCH_CANCEL_ORDERS = OP_BATCH_CANCEL_ORDERS;
    String EVENT_ERROR = "error";

    String CHANNEL_TICKERS = "tickers";
    String CHANNEL_ORDERS = "orders";
    String CHANNEL_CANDLE = "candle" + CANDLE_BAR;

    Set<String> PRIVATE_CHANNELS = new HashSet<>(Arrays.asList("orders", "balance"));

    String GET_CANDLE_HISTORY = "/api/v5/market/candles";
    String GET_LEVER = "/api/v5/account/leverage-info";
    String SET_LEVER = "/api/v5/account/set-leverage";
    String GET_BALANCE = "/api/v5/account/balance";
    String ADD_MARGIN = "/api/v5/account/position/margin-balance";

    String GET_ORDER_HISTORY = "/api/v5/trade/orders-history";
    String GET_PENDING_ORDER = "/api/v5/trade/orders-pending";
    String GET_PENDING_ALGO = "/api/v5/trade/orders-algo-pending";
    String PLACE_ALGO = "/api/v5/trade/order-algo";
    String CANCEL_ALGO = "/api/v5/trade/cancel-algos";
    String CLOSE_POSITION = "/api/v5/trade/close-position";

}
