package org.iushu.trader.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class Configuration {

    private static final String DEFAULT_PROPERTIES_FILE = "global.properties";
    private final Properties properties = new Properties();
    private static final Configuration INSTANCE = new Configuration();

    private Configuration() {
        URL resource = Configuration.class.getClassLoader().getResource(DEFAULT_PROPERTIES_FILE);
        try {
            properties.load(new FileInputStream(resource.getFile()));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
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

}
