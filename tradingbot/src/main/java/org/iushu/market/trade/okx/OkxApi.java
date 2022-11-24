package org.iushu.market.trade.okx;

public interface OkxApi {

    String GET_CANDLE_HISTORY = "/api/v5/market/candles";
    String GET_LEVER = "/api/v5/account/leverage-info";

    String CANCEL_ALGO = "/api/v5/trade/cancel-algos";

}
