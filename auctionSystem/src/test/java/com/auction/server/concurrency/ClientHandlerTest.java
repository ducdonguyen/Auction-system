package com.auction.server.concurrency;

import com.auction.server.core.AuctionService;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    @Mock
    private Socket socket;

    @Mock
    private AuctionService auctionService;

    @Mock
    private ObjectOutputStream out;

    private ClientHandler clientHandler;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        clientHandler = new ClientHandler(socket, auctionService);
        
        // Inject the mocked output stream using reflection since it's private and initialized in run()
        java.lang.reflect.Field outField = ClientHandler.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(clientHandler, out);
    }

    @Test
    public void testUpdateNewBid() throws Exception {
        BidTransaction bid = new BidTransaction("TX1", new Bidder("user1", "", 0), 1000.0, LocalDateTime.now());
        
        clientHandler.updateNewBid("AUC001", bid);
        
        verify(out).writeObject(bid);
        verify(out).flush();
    }

    @Test
    public void testUpdateStatus() throws Exception {
        AuctionStatus status = AuctionStatus.RUNNING;
        
        clientHandler.updateStatus("AUC001", status);
        
        verify(out).writeObject(status);
        verify(out).flush();
    }

    @Test
    public void testUpdateStatusWhenOutIsNull() throws Exception {
        // Set out to null
        java.lang.reflect.Field outField = ClientHandler.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(clientHandler, null);
        
        clientHandler.updateStatus("AUC001", AuctionStatus.FINISHED);
        
        // Should not throw exception
        verifyNoInteractions(out);
    }
}
