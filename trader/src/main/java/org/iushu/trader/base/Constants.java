package org.iushu.trader.base;

import java.util.Properties;

public interface Constants {

    String KEY_EXCHANGE = "exchange";
    String KEY_API_URL = "api.url.";
    String KEY_WS_PUBLIC_URL = "ws.public.url.";
    String KEY_WS_PRIVATE_URL = "ws.private.url.";

    String OKX_API_URL = "https://www.okx.com";
    String OKX_WSS_PUBLIC_URL = "wss://wspap.okx.com:8443/ws/v5/public?brokerId=9999";
    String OKX_WSS_PRIVATE_URL = "wss://wspap.okx.com:8443/ws/v5/private?brokerId=9999";
    String OKX_WSS_REQUEST_PATH = "/users/self/verify";

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

}
