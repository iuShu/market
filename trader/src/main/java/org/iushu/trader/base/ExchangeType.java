package org.iushu.trader.base;

import org.iushu.trader.okx.OkxWsJsonClient;

public enum ExchangeType {

    OKX("okx", OkxWsJsonClient.class),

    BINANCE("bnb", Object.class),   // not implemented

    ;

    public String name;
    public Class<?> clientClass;

    ExchangeType(String name, Class<?> clientClass) {
        this.name = name;
        this.clientClass = clientClass;
    }

    public String restUrl() {
        return Configuration.getString(Constants.KEY_API_URL + name);
    }

    public String wsPublicUrl() {
        return Configuration.getString(Constants.KEY_WS_PUBLIC_URL + name);
    }

    public String wsPrivateUrl() {
        return Configuration.getString(Constants.KEY_WS_PRIVATE_URL + name);
    }

    public static ExchangeType get() {
        String exchange = Configuration.getString(Constants.KEY_EXCHANGE);
        return valueOf(exchange.toUpperCase());
    }

}
