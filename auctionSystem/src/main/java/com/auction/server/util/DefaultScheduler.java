package com.auction.server.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultScheduler implements IScheduler {
    private final ScheduledExecutorService executor;

    public DefaultScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
}