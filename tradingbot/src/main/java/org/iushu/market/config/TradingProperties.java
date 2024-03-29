package org.iushu.market.config;

import org.iushu.market.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ConfigurationProperties(prefix = "trade")
public class TradingProperties {

    private String exchange;
    private String currency;
    private String inst;
    private String instId;
    private String instType;
    private String tdMode;
    private int lever;
    private long heartbeatPeriod = 10000;   // millisecond
    private String stopFile;
    private String longFile;
    private String shortFile;

    private final TradingProperties.Notify notify = new TradingProperties.Notify();
    private final TradingProperties.Okx okx = new TradingProperties.Okx();
    private final TradingProperties.Bnb bnb = new TradingProperties.Bnb();

    private final OrderProperties order = new OrderProperties();

    public static class Notify {

        private boolean windows;
        private boolean robot;
        private String webhook;
        private String secret;
        private int rateLimited;
        private long limitedPeriod;

        public boolean isWindows() {
            return windows;
        }

        public void setWindows(boolean windows) {
            this.windows = windows;
        }

        public boolean isRobot() {
            return robot;
        }

        public void setRobot(boolean robot) {
            this.robot = robot;
        }

        public String getWebhook() {
            return webhook;
        }

        public void setWebhook(String webhook) {
            this.webhook = webhook;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getRateLimited() {
            return rateLimited;
        }

        public void setRateLimited(int rateLimited) {
            this.rateLimited = rateLimited;
        }

        public long getLimitedPeriod() {
            return limitedPeriod;
        }

        public void setLimitedPeriod(long limitedPeriod) {
            this.limitedPeriod = limitedPeriod;
        }
    }

    public interface ApiInfo {
        String getApi();
        String getWsPublicUrl();
        String getWsPrivateUrl();
        String getApiKey();
        String getSecret();
        String getPassphrase();
    }

    public static class Okx implements ApiInfo {

        private String api;
        private String wsPublicUrl;
        private String wsPrivateUrl;
        private String apiKey;
        private String secret;
        private String passphrase;

        public String getApi() {
            return api;
        }

        public void setApi(String api) {
            this.api = api;
        }

        public String getWsPublicUrl() {
            return wsPublicUrl;
        }

        public void setWsPublicUrl(String wsPublicUrl) {
            this.wsPublicUrl = wsPublicUrl;
        }

        public String getWsPrivateUrl() {
            return wsPrivateUrl;
        }

        public void setWsPrivateUrl(String wsPrivateUrl) {
            this.wsPrivateUrl = wsPrivateUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getPassphrase() {
            return passphrase;
        }

        public void setPassphrase(String passphrase) {
            this.passphrase = passphrase;
        }

        @Override
        public String toString() {
            return "Okx{" +
                    "api='" + api + '\'' +
                    ", wsPublicUrl='" + wsPublicUrl + '\'' +
                    ", wsPrivateUrl='" + wsPrivateUrl + '\'' +
                    ", apiKey='" + apiKey + '\'' +
                    ", secret='" + secret + '\'' +
                    ", passphrase='" + passphrase + '\'' +
                    '}';
        }
    }

    public static class Bnb implements ApiInfo {

        private String api;
        private String wsPublicUrl;
        private String wsPrivateUrl;
        private String apiKey;
        private String secret;
        private String passphrase;

        public String getApi() {
            return api;
        }

        public void setApi(String api) {
            this.api = api;
        }

        public String getWsPublicUrl() {
            return wsPublicUrl;
        }

        public void setWsPublicUrl(String wsPublicUrl) {
            this.wsPublicUrl = wsPublicUrl;
        }

        public String getWsPrivateUrl() {
            return wsPrivateUrl;
        }

        public void setWsPrivateUrl(String wsPrivateUrl) {
            this.wsPrivateUrl = wsPrivateUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getPassphrase() {
            return passphrase;
        }

        public void setPassphrase(String passphrase) {
            this.passphrase = passphrase;
        }

        @Override
        public String toString() {
            return "Bnb{" +
                    "api='" + api + '\'' +
                    ", wsPublicUrl='" + wsPublicUrl + '\'' +
                    ", wsPrivateUrl='" + wsPrivateUrl + '\'' +
                    ", apiKey='" + apiKey + '\'' +
                    ", secret='" + secret + '\'' +
                    ", passphrase='" + passphrase + '\'' +
                    '}';
        }
    }

    @Bean
    @Profile(Constants.EXChANGE_OKX)
    public TradingProperties.ApiInfo okxApiInfo(TradingProperties properties) {
        return properties.getOkx();
    }

    @Bean
    @Profile(Constants.EXChANGE_BNB)
    public TradingProperties.ApiInfo bnbApiInfo(TradingProperties properties) {
        return properties.getBnb();
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public String getInst() {
        return inst;
    }

    public void setInst(String inst) {
        this.inst = inst;
    }

    public String getInstType() {
        return instType;
    }

    public void setInstType(String instType) {
        this.instType = instType;
    }

    public String getTdMode() {
        return tdMode;
    }

    public void setTdMode(String tdMode) {
        this.tdMode = tdMode;
    }

    public int getLever() {
        return lever;
    }

    public void setLever(int lever) {
        this.lever = lever;
    }

    public long getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    public void setHeartbeatPeriod(long heartbeatPeriod) {
        this.heartbeatPeriod = heartbeatPeriod;
    }

    public String getStopFile() {
        return stopFile;
    }

    public void setStopFile(String stopFile) {
        this.stopFile = stopFile;
    }

    public Notify getNotify() {
        return notify;
    }

    public TradingProperties.ApiInfo getOkx() {
        return this.okx;
    }

    public TradingProperties.ApiInfo getBnb() {
        return this.bnb;
    }

    public OrderProperties getOrder() {
        return order;
    }

    public String getLongFile() {
        return longFile;
    }

    public void setLongFile(String longFile) {
        this.longFile = longFile;
    }

    public String getShortFile() {
        return shortFile;
    }

    public void setShortFile(String shortFile) {
        this.shortFile = shortFile;
    }
}
