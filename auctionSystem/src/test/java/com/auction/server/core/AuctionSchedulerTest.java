package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import com.auction.shared.models.auction.Auction;
import com.auction.shared.models.auction.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuctionSchedulerTest {

    @Mock
    private AuctionRepository repository;
    @Mock
    private AuctionService auctionService;
    @Mock
    private ScheduledExecutorService scheduler;

    private AuctionScheduler auctionScheduler;

    @BeforeEach
    void setUp() {
        auctionScheduler = new AuctionScheduler(repository, auctionService, scheduler);
    }

    @Test
    @DisplayName("Kiểm thử bắt đầu lập lịch")
    void testStartScheduling() {
        auctionScheduler.startScheduling();
        verify(scheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Kiểm thử logic autoUpdateAuctions - Mở và Đóng phiên")
    void testAutoUpdateAuctions() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        auctionScheduler.startScheduling();
        verify(scheduler).scheduleAtFixedRate(runnableCaptor.capture(), anyLong(), anyLong(), any(TimeUnit.class));

        Runnable capturedTask = runnableCaptor.getValue();

        Auction auctionToStart = mock(Auction.class);
        Auction auctionToFinish = mock(Auction.class);

        when(repository.findByStatusAndStartTimeBefore(eq(AuctionStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(auctionToStart));
        when(repository.findByStatusAndEndTimeBefore(eq(AuctionStatus.RUNNING), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(auctionToFinish));

        capturedTask.run();

        verify(auctionService).updateAuctionStatus(auctionToStart, AuctionStatus.RUNNING);
        verify(auctionService).updateAuctionStatus(auctionToFinish, AuctionStatus.FINISHED);
    }
}
