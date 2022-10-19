package org.iushu.trader.base;

import java.io.IOException;

public class NotifyUtil {

    public static final String VALVE_KEY = "notify.open";

    public static void windowTips(String title, String message) {
        if (Configuration.getBoolean(VALVE_KEY))
            execCommand(String.format("mshta vbscript:msgbox(\"%s\",64,\"%s\")(window.close)", message, title));
    }

    public static void windowVoiceSpeak(String message) {
        if (Configuration.getBoolean(VALVE_KEY))
            execCommand(String.format("mshta vbscript:createobject(\"sapi.spvoice\").speak(\"%s\")(window.close)", message));
    }

    public static void windowTipsAndVoice(String title, String message) {
        if (!Configuration.getBoolean(VALVE_KEY))
            return;
        DefaultExecutor.executor().submit(() -> {
            windowTips(title, message);
            windowVoiceSpeak(message);
        });
    }

    private static void execCommand(String command) {
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException("windows command execute failed");
        }
    }

    public static void main(String[] args) {
        String title = "Order Filled Notification";
        String message = "Order has been filled, price 19268.34, position 10";
//        windowTips(title, message);
        windowTipsAndVoice(title, message);
    }

}
