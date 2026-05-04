package com.auction.server.core;
import org.slf4j.*;
import com.auction.shared.exceptions.AuthenticationException;
import com.auction.shared.models.*;
import java.util.*;
import java.util.concurrent.*;
public class AuctionManager {
    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);
    private static volatile AuctionManager instance;
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final Map<String, List<AuctionObserver>> observersMap = new ConcurrentHashMap<>();
    private AuctionManager() {}
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) instance = new AuctionManager(); } }
        return instance;
    }
    public void addAuction(Auction a, String t) throws AuthenticationException {
        if (t == null || !t.equals("ADMIN_SECRET_TOKEN")) throw new AuthenticationException("Invalid token.");
        if (a == null || a.getAuctionId() == null) throw new IllegalArgumentException("Invalid auction.");
        if (activeAuctions.putIfAbsent(a.getAuctionId(), a) != null) throw new IllegalArgumentException("Auction exists.");
    }
    public void subscribe(String aid, AuctionObserver o) {
        observersMap.computeIfAbsent(aid, k -> new CopyOnWriteArrayList<>()).add(o);
        logger.info("Subscribed to: {}", aid);
    }
    public void unsubscribe(String aid, AuctionObserver o) {
        List<AuctionObserver> obs = observersMap.get(aid);
        if (obs != null) { obs.remove(o); logger.info("Unsubscribed from: {}", aid); }
    }
    public void notifyObservers(String aid, BidTransaction b) {
        List<AuctionObserver> obs = observersMap.get(aid);
        if (obs != null) obs.forEach(o -> o.updateNewBid(aid, b));
    }
    public void notifyStatusUpdate(String aid, AuctionStatus s) {
        List<AuctionObserver> obs = observersMap.get(aid);
        if (obs != null) obs.forEach(o -> o.updateStatus(aid, s));
    }
}
