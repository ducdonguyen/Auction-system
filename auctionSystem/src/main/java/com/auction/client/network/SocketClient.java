package com.auction.client.network;
import org.slf4j.*;
import com.auction.shared.models.AuctionStatus;
import com.auction.shared.models.BidTransaction;
import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
public class SocketClient {
    private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);
    private static volatile SocketClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();
    private final AtomicReference<RealtimeListener> realtimeListener = new AtomicReference<>();
    public interface RealtimeListener { void onNewBid(BidTransaction b); void onStatusUpdate(AuctionStatus s); }
    private SocketClient() {}
    public static SocketClient getInstance() {
        if (instance == null) { synchronized (SocketClient.class) { if (instance == null) instance = new SocketClient(); } }
        return instance;
    }
    public void setRealtimeListener(RealtimeListener l) { this.realtimeListener.set(l); }
    private void startListeningThread() {
        Thread t = new Thread(() -> {
            try { while (!Thread.currentThread().isInterrupted()) handle(in.readObject()); }
            catch (Exception e) { logger.info("[Client] Listener stopped: {}", e.getMessage()); }
        });
        t.setDaemon(true); t.start();
    }
    private void handle(Object m) throws InterruptedException {
        if (m instanceof BidTransaction b) { RealtimeListener l = realtimeListener.get(); if (l != null) Platform.runLater(() -> l.onNewBid(b)); }
        else if (m instanceof AuctionStatus s) { RealtimeListener l = realtimeListener.get(); if (l != null) Platform.runLater(() -> l.onStatusUpdate(s)); }
        else responseQueue.put(m);
    }
    public synchronized void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket("localhost", 8080);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            logger.info("[Client] Connected to Server.");
            startListeningThread();
        }
    }
    public synchronized void sendRequest(Object r) throws IOException { if (out != null) { out.writeObject(r); out.flush(); } }
    public Object receiveResponse() throws InterruptedException { return responseQueue.take(); }
    public synchronized void disconnect() {
        try { if (in != null) in.close(); if (out != null) out.close(); if (socket != null) socket.close(); logger.info("[Client] Disconnected."); }
        catch (IOException e) { logger.error("[Client] Disconnect error: {}", e.getMessage()); }
    }
}
