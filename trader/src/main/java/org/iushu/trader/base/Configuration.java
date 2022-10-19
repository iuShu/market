package org.iushu.trader.base;

import com.alibaba.fastjson2.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Configuration {

    private static final String DEFAULT_PROPERTIES_FILE = "global.properties";
    private static final Configuration INSTANCE = new Configuration();
    private Properties properties;
    private long lastModifyTime = 0L;

    private Configuration() {
        load();
        if (this.properties.isEmpty())
            System.exit(1);
        watching();
    }

    private void load() {
        FileInputStream inputStream = null;
        try {
            URL resource = Configuration.class.getClassLoader().getResource(DEFAULT_PROPERTIES_FILE);
            if (resource == null)
                throw new NullPointerException("can not found configuration file at " + DEFAULT_PROPERTIES_FILE);

            File config = new File(resource.getFile());
            if (this.lastModifyTime != 0L && this.lastModifyTime == config.lastModified())
                return;

            inputStream = new FileInputStream(config);
            Properties properties = new Properties();
            properties.load(inputStream);
            if (!properties.isEmpty())
                this.properties = properties;
            this.lastModifyTime = config.lastModified();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(inputStream);
        }
    }

    private void watching() {
        DefaultExecutor.scheduler().scheduleAtFixedRate(this::load, 0, 5, TimeUnit.SECONDS);
    }

    public static int getInt(String key) {
        return getInt(key, -1);
    }

    public static int getInt(String key, int defaultValue) {
        String val = getString(key, "");
        if (val == null || val.trim().length() < 1)
            return defaultValue;
        return Integer.parseInt(val);
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String defaultValue) {
        return INSTANCE.properties.getProperty(key, defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

}
