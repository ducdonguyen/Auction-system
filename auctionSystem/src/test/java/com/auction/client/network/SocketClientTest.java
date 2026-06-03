package com.auction.client.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class SocketClientTest {

    @AfterEach
    void cleanup() {
        SocketClient.setInstance(null);
    }

    @Test
    void getInstance_shouldReturnSingleton() {
        SocketClient s1 = SocketClient.getInstance();
        SocketClient s2 = SocketClient.getInstance();

        assertNotNull(s1);
        assertSame(s1, s2);
    }

    @Test
    void setInstance_shouldReplaceSingleton() {
        SocketClient original = SocketClient.getInstance();

        SocketClient replacement = SocketClient.getInstance();
        SocketClient.setInstance(replacement);

        assertSame(replacement, SocketClient.getInstance());
        assertNotNull(original);
    }

    @Test
    void receiveResponse_shouldReturnQueuedObject() throws Exception {
        SocketClient client = SocketClient.getInstance();

        Field field = SocketClient.class.getDeclaredField("responseQueue");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        BlockingQueue<Object> queue =
                (BlockingQueue<Object>) field.get(client);

        queue.offer("hello");

        Object response = client.receiveResponse(1);

        assertEquals("hello", response);
    }

    @Test
    void receiveResponse_shouldThrowTimeoutException() {
        SocketClient client = SocketClient.getInstance();

        assertThrows(
                TimeoutException.class,
                () -> client.receiveResponse(1)
        );
    }

    @Test
    void receiveResponseDefault_shouldReturnObject() throws Exception {
        SocketClient client = SocketClient.getInstance();

        Field field = SocketClient.class.getDeclaredField("responseQueue");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        BlockingQueue<Object> queue =
                (BlockingQueue<Object>) field.get(client);

        queue.offer("response");

        Object result = client.receiveResponse();

        assertEquals("response", result);
    }

    @Test
    void registerResponseHandler_shouldNotThrow() {
        SocketClient client = SocketClient.getInstance();

        assertDoesNotThrow(() ->
                client.registerResponseHandler(
                        String.class,
                        o -> {
                        }
                )
        );
    }

    @Test
    void setRealtimeListener_shouldNotThrow() {
        SocketClient client = SocketClient.getInstance();

        SocketClient.RealtimeListener listener =
                new SocketClient.RealtimeListener() {
                    @Override
                    public void onNewBid(
                            com.auction.shared.models.auction.BidTransaction b) {
                    }

                    @Override
                    public void onStatusUpdate(
                            com.auction.shared.models.auction.AuctionStatus s) {
                    }

                    @Override
                    public void onBalanceUpdate(
                            double newBalance,
                            double amountChanged,
                            String reason) {
                    }

                    @Override
                    public void onTimeUpdate(long newEndMillis) {
                    }
                };

        assertDoesNotThrow(() ->
                client.setRealtimeListener(listener)
        );
    }

    @Test
    void disconnect_shouldNotThrowWhenNeverConnected() {
        SocketClient client = SocketClient.getInstance();

        assertDoesNotThrow(client::disconnect);
    }

    @Test
    void disconnect_shouldHandleNullStreams() {
        SocketClient client = SocketClient.getInstance();

        assertDoesNotThrow(client::disconnect);
    }

    @Test
    void singleton_shouldRemainStableAcrossCalls() {
        SocketClient first = SocketClient.getInstance();

        for (int i = 0; i < 20; i++) {
            assertSame(first, SocketClient.getInstance());
        }
    }
}