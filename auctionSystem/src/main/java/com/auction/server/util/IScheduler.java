package com.auction.server.util;

import java.util.concurrent.TimeUnit;

public interface IScheduler {
    void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);
}