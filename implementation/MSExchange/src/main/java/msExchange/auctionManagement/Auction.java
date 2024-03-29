package msExchange.auctionManagement;

import CF.sendable.Bid;
import CF.sendable.Sell;
import CF.sendable.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class Auction {
    private static final Logger logger = LogManager.getLogger(Auction.class);
    private UUID auctionID;
    private UUID bidderID;
    private UUID sellerID;
    private UUID timeSlotID;
    private double pricePerWh;
    private double totalVolume;
    private double soldVolume = 0;
    private boolean auctionEnded = false;
    private BlockingQueue<Transaction> transactionQueue;

    public Auction(UUID auctionID, Sell sellPosition, BlockingQueue<Transaction> transactionQueue) {
        this.auctionID = auctionID;
        this.sellerID = sellPosition.getSellerID();
        this.pricePerWh = sellPosition.getAskPrice();
        this.totalVolume = sellPosition.getVolume();
        this.transactionQueue = transactionQueue;
        this.timeSlotID = sellPosition.getTimeSlot();
    }

    public void setBid(Bid bidPosition) {
        if (!auctionEnded) {
            if (this.bidderID != null) {
                if (bidPosition.getPrice() > pricePerWh) {
                    this.pricePerWh = bidPosition.getPrice();
                    this.bidderID = bidPosition.getBidderID();
                    this.soldVolume = bidPosition.getVolume();
                }
            } else {
                this.pricePerWh = bidPosition.getPrice();
                this.bidderID = bidPosition.getBidderID();
                this.soldVolume = bidPosition.getVolume();
            }
        }
    }

    public void endAuction() {
        //Create a transaction and add it to blockingQueue
        logger.info("EXCHANGE: Auction " + auctionID + " has ended for timeSlot " + timeSlotID);
        auctionEnded = true;
        if (soldVolume != 0 && bidderID != null) {
            logger.debug("EXCHANGE: Auction information: auctionID: {} bidderID: {} soldVolume: {} pricePerWh{}", auctionID, bidderID, soldVolume, pricePerWh);
            Transaction transaction = new Transaction(sellerID, bidderID, soldVolume, pricePerWh, auctionID);
            try {
                transactionQueue.put(transaction);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public boolean isAuctionEnded() {
        return this.auctionEnded;
    }

    public double getPrice() {
        return pricePerWh;
    }

    public UUID getAuctionId() {
        return auctionID;
    }

    public UUID getBidderID() {
        return bidderID;
    }

    public double getVolume() {
        return this.totalVolume;
    }

    public UUID getTimeSlotID() {
        return this.timeSlotID;
    }
}
