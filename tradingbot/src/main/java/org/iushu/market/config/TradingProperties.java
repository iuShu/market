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
    private String instId;
    private String tdMode;
    private int lever;
    private long heartbeatPeriod = 20000;   // millisecond

    private final TradingProperties.Okx okx = new TradingProperties.Okx();
    private final TradingProperties.Bnb bnb = new TradingProperties.Bnb();

    public static class Notify {

        private boolean windows;
        private boolean robot;
        private boolean webhook;
        private boolean secret;

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

        public boolean isWebhook() {
            return webhook;
        }

        public void setWebhook(boolean webhook) {
            this.webhook = webhook;
        }

        public boolean isSecret() {
            return secret;
        }

        public void setSecret(boolean secret) {
            this.secret = secret;
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

    public TradingProperties.ApiInfo getOkx() {
        return this.okx;
    }

    public TradingProperties.ApiInfo getBnb() {
        return this.bnb;
    }

}
