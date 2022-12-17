package org.iushu.market.component;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProfileContext implements ApplicationContextAware {

    public static final String TEST = "test";
    public static final String SHADOW = "shadow";
    public static final String PROD = "prod";

    private static final JSONObject profiles = JSONObject.of();

    public static boolean isProfile(String profile) {
        return profiles.getBooleanValue(profile, false);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Arrays.stream(applicationContext.getEnvironment().getActiveProfiles()).forEach(p -> {
            profiles.put(p, true);
        });
    }
}
