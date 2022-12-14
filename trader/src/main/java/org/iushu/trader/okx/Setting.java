package org.iushu.trader.okx;

import org.iushu.trader.base.Configuration;
import org.iushu.trader.base.Constants;
import org.iushu.trader.base.ExchangeType;
import org.iushu.trader.base.PosSide;

import java.math.BigDecimal;

public interface Setting {

    String ENV = Configuration.getString("env");

    String APIKEY = Configuration.getString("api.key." + ExchangeType.OKX.name + "." + ENV);
    String SECRETKEY = Configuration.getString("api.secret." + ExchangeType.OKX.name + "." + ENV);
    String PASSPHRASE = Configuration.getString("api.passphrase." + ExchangeType.OKX.name + "." + ENV);

    String API_URL = Configuration.getString("api.url." + ExchangeType.OKX.name + "." + ENV);
    String WSS_PUBLIC_URL = Configuration.getString("ws.public.url." + ExchangeType.OKX.name + "." + ENV);
    String WSS_PRIVATE_URL = Configuration.getString("ws.private.url." + ExchangeType.OKX.name + "." + ENV);

    String WSS_PUB_CHANNEL_TICKERS = "tickers";
    String WSS_PUB_CHANNEL_CANDLE = "candle";
    String WSS_PRV_CHANNEL_ORDERS = "orders";
    String WSS_PRV_CHANNEL_ORDER_ALGO = "orders-algo";
    String WSS_PRV_CHANNEL_POSITIONS = "positions";
    String WSS_PRV_CHANNEL_ACCOUNT = "account";

    String API_CANDLE_DATA = "/api/v5/market/candles";
    String API_LEVERAGE = "/api/v5/account/leverage-info";
    String API_SET_LEVERAGE = "/api/v5/account/set-leverage";
    String API_BALANCE = "/api/v5/account/balance";
    String API_MARGIN_BALANCE = "/api/v5/account/position/margin-balance";
    String API_CLOSE_POSITION = "/api/v5/trade/close-position";
    String API_ALGO_ORDER = "/api/v5/trade/order-algo";
    String API_CANCEL_ALGO = "/api/v5/trade/cancel-algos";

    ExchangeType EXCHANGE = ExchangeType.OKX;
    String INST_ID = Constants.INST_ID_BTC_USDT_SWAP;
    String INST_TYPE = Constants.INST_TYPE_SWAP;
    String CANDLE_TYPE = Constants.CANDLE_TYPE_1M;
    int CANDLE_TYPE_MILLISECONDS = 60000;
    String TD_MODE = Configuration.getString("position.mode");
    String CURRENCY = Configuration.getString("trade.currency");

    OkxChannel CHANNEL_TICKERS = new OkxChannel(WSS_PUB_CHANNEL_TICKERS, INST_ID);
    OkxChannel CHANNEL_CANDLE = new OkxChannel(WSS_PUB_CHANNEL_CANDLE + CANDLE_TYPE, INST_ID);
    OkxChannel CHANNEL_ORDERS = new OkxChannel(WSS_PRV_CHANNEL_ORDERS, INST_ID, INST_TYPE);
    OkxChannel CHANNEL_ORDERS_ALGO = new OkxChannel(WSS_PRV_CHANNEL_ORDER_ALGO, INST_ID, INST_TYPE);
    OkxChannel CHANNEL_POSITIONS = new OkxChannel(WSS_PRV_CHANNEL_POSITIONS, INST_ID, INST_TYPE);
    OkxChannel CHANNEL_ACCOUNT = new OkxChannel(WSS_PRV_CHANNEL_ACCOUNT);

    int COOLING_DOWN_MILLISECONDS = 10000;
    int STRATEGY_MA_TYPE = 10;
    int STRATEGY_EMA_TYPE = 12;
//    int STRATEGY_EMA_TYPE = 144;
    PosSide POS_SIDE = PosSide.ShortSide;
    String SIDE_OPEN = Constants.SIDE_SELL;
    String SIDE_CLOSE = Constants.SIDE_BUY;
    int ORDER_LEVER = Configuration.getInt(String.format("position.%s.lever", TD_MODE));
    int ORDER_START_POS = 10;
    double ORDER_TAKE_PROFIT_RATE = 1.0;
    double ORDER_FOLLOW_RATE = 0.005;
    double ORDER_PROFIT_STEP_RATE = 0.0002;     // deprecated
    double ORDER_FEE_RATE = 0.0005;
    int ORDER_MAX_ORDERS = 4;
    int ORDER_CLOSE_PX_THRESHOLD = 3;
    int OPERATION_MAX_FAILURE_TIMES = 5;

}
