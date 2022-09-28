package org.iushu.trader.base;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultExecutor {

    private static final String THREAD_POOL_NAME = "trader";
    private final ExecutorService executor;
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private static final DefaultExecutor INSTANCE = new DefaultExecutor();

    private DefaultExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadFactory factory = new CustomizedThreadFactory();
        executor = new ThreadPoolExecutor(processors, processors * 2,
                60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                factory, new ThreadPoolExecutor.AbortPolicy());
    }

    public static ExecutorService executor() {
        return INSTANCE.executor;
    }

    public static ScheduledExecutorService scheduler() {
        return scheduledExecutor;
    }

    static class CustomizedThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final String namePrefix;

        public CustomizedThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = THREAD_POOL_NAME + "-th-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
