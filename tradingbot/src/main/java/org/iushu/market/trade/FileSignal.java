package org.iushu.market.trade;

import org.iushu.market.config.TradingProperties;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileSignal {

    private final TradingProperties properties;

    public FileSignal(TradingProperties properties) {
        this.properties = properties;
    }

    public boolean isStop() {
        String stopFile = properties.getStopFile();
        if (stopFile == null || stopFile.trim().isEmpty())
            return false;
        return new File(stopFile).exists();
    }

    public PosSide manualSide() {
        String longFile = properties.getLongFile();
        String shortFile = properties.getShortFile();
        if (longFile != null && !longFile.trim().isEmpty() && new File(longFile).exists())
            return PosSide.LongSide;
        if (shortFile != null && !shortFile.trim().isEmpty() && new File(shortFile).exists())
            return PosSide.ShortSide;
        return null;
    }

}
