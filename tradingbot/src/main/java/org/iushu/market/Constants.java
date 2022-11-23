package org.iushu.market;

public interface Constants {

    String EXChANGE_OKX = "okx";
    String EXChANGE_BNB = "bnb";

    String INST_TYPE_SPOT = "SPOT";
    String INST_TYPE_MARGIN = "MARGIN";
    String INST_TYPE_SWAP = "SWAP";
    String INST_TYPE_FUTURES = "FUTURES";
    String INST_TYPE_OPTION = "OPTION";
    
    String INST_ID_BTC_USDT_SWAP = "BTC-USDT-SWAP";

    String CANDLE_TYPE_1M = "1m";
    String CANDLE_TYPE_3M = "3m";
    String CANDLE_TYPE_5M = "5m";
    String CANDLE_TYPE_15M = "15m";
    String CANDLE_TYPE_30M = "30m";
    String CANDLE_TYPE_1H = "1H";
    String CANDLE_TYPE_2H = "2H";
    String CANDLE_TYPE_4H = "4H";

    String ORDER_TYPE_MARKET = "market";
    String ORDER_TYPE_LIMIT = "limit";

    String SIDE_SELL = "sell";
    String SIDE_BUY = "buy";

    String POS_SIDE_LONG = "long";
    String POS_SIDE_SHORT = "short";

    String TD_MODE_ISOLATED = "isolated";
    String TD_MODE_CROSS = "cross";

    String ORDER_STATE_LIVE = "live";
    String ORDER_STATE_FILLED = "filled";
    String ORDER_STATE_CANCELED = "canceled";

    String ALGO_TYPE_CONDITIONAL = "conditional";
    String ALGO_TYPE_OCO = "oco";
    String ALGO_TYPE_TRIGGER = "trigger";
    String ALGO_TYPE_MOVE_ORDER_STOP = "move_order_stop";
    String ALGO_TYPE_ICEBERG = "iceberg";
    String ALGO_TYPE_TWAP = "twap";

    String ALGO_PX_TYPE_LAST = "last";
    String ALGO_PX_TYPE_MARK = "mark";
    String ALGO_PX_TYPE_INDEX = "index";

}
