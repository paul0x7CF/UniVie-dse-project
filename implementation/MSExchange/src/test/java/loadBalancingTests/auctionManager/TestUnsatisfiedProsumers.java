package loadBalancingTests.auctionManager;

import CF.sendable.Bid;
import CF.sendable.Sell;
import MSP.Exceptions.InvalidBidException;
import MSP.Exceptions.InvalidTimeSlotException;
import loadManager.SellInformation;
import loadManager.auctionManagement.Auction;
import loadManager.auctionManagement.AuctionManager;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.*;

public class TestUnsatisfiedProsumers {
    private double volume = 100;
    private double price = 100;
    private UUID timeSlotID = UUID.randomUUID();

    @Test
    public void givenTimeSlot_getUnsatisfiedSellers_expectedCorrectList() {
        //arrange
        AuctionManager auctionManager = new AuctionManager();
        List<UUID> unsatisfiedSellers = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Sell sell = new Sell(volume, price, timeSlotID, UUID.randomUUID());
            SellInformation sellInformation = new SellInformation(sell, UUID.randomUUID());
            Auction auction = new Auction(UUID.randomUUID(), sellInformation);
            auctionManager.addAuction(auction);

            if (i < 5) {
                Bid bid = new Bid(volume, price, timeSlotID, UUID.randomUUID());
                try {
                    auctionManager.setBidder(auction.getAuctionId(), bid);
                } catch (InvalidBidException e) {
                    throw new RuntimeException(e);
                }
            } else {
                //add unsatisfied sellers
                unsatisfiedSellers.add(sell.getSellerID());
            }
        }

        //act
        Map<UUID, Double> actualUnsatisfiedSellers = new HashMap<>();
        try {
            actualUnsatisfiedSellers = auctionManager.getUnsatisfiedSellers(timeSlotID);
        } catch (InvalidTimeSlotException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(unsatisfiedSellers.size(), actualUnsatisfiedSellers.size());

        int i = 0;
        for (UUID unsatisfiedSeller : unsatisfiedSellers) {
            for (UUID actualUnsatisfiedSeller : actualUnsatisfiedSellers.keySet()) {
                if (unsatisfiedSeller.equals(actualUnsatisfiedSeller)) {
                    i++;
                }
            }
        }

        Assertions.assertEquals(i, unsatisfiedSellers.size());
    }

    @Test
    public void noAuctionsInSlot_getUnsatisfiedSellers_expectedEmptyList() {
        //arrange
        AuctionManager auctionManager = new AuctionManager();
        List<UUID> timeSlots = new ArrayList<>();
        timeSlots.add(timeSlotID);

        auctionManager.addNewTimeSlots(timeSlots);

        //act
        Map<UUID, Double> actualUnsatisfiedSellers = null;
        try {
            actualUnsatisfiedSellers = auctionManager.getUnsatisfiedSellers(timeSlotID);
        } catch (InvalidTimeSlotException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(0, actualUnsatisfiedSellers.size());
    }
}
