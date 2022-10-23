package org.iushu.trader;

import java.util.ArrayList;
import java.util.List;

public class SyncController {

    private final List<SyncControl> syncControls = new ArrayList<>();

    private static final SyncController INSTANCE = new SyncController();

    private SyncController() {}

    public static SyncController instance() {
        return INSTANCE;
    }

    public void register(SyncControl client) {
        syncControls.add(client);
    }

    public void shutdown() {
        syncControls.forEach(SyncControl::syncShutdown);
        Trader.instance().stop();
//        RealTraderTest.instance().stop();
    }

}
