package msExchange.messageHandling;

import Exceptions.InvalidBidException;
import Exceptions.InvalidSellException;
import Exceptions.InvalidTimeSlotException;
import exceptions.MessageProcessingException;
import mainPackage.ESubCategory;
import messageHandling.IMessageHandler;
import msExchange.auctionManagement.AuctionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocol.Message;
import sendable.Bid;
import sendable.Sell;
import sendable.TimeSlot;
import sendable.Transaction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ExchangeMessageHandler implements IMessageHandler {
    private static final Logger logger = LogManager.getLogger(ExchangeMessageHandler.class);
    private BlockingQueue<Bid> bidQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Sell> sellQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();
    private AuctionManager auctionManager;

    public ExchangeMessageHandler() {
        auctionManager = new AuctionManager(transactionQueue, bidQueue, sellQueue);
        Thread auctionManagerThread = new Thread(auctionManager);
        auctionManagerThread.start();
    }

    /**
     * Handles the incoming message and performs the appropriate actions based on the message's subcategory.
     *
     * @param message The incoming message to handle
     * @throws MessageProcessingException if there is an error processing the message
     */
    public void handleMessage(Message message) throws MessageProcessingException {
        String subcategory = message.getSubCategory();
        if (subcategory.contains(";")) {
            throw new MessageProcessingException("Subcategory has another subcategory: " + subcategory);
        }

        ESubCategory subCategory = ESubCategory.valueOf(subcategory);

        try {
            switch (subCategory) {
                case Bid -> handleBid(message);
                case Sell -> handleSell(message);
                case TimeSlot -> handleTimeSlot(message);
                default ->
                        throw new MessageProcessingException("Unknown message subCategory: " + message.getSubCategory());
            }
        } catch (InvalidBidException | InvalidSellException | InvalidTimeSlotException e) {
            throw new MessageProcessingException(e.getMessage());
        }

        logger.trace("{} Message processed", message.getCategory());
    }

    /**
     * Handles the TimeSlot message by validating the TimeSlot and adding it to the AuctionManager.
     *
     * @param message The TimeSlot message to handle
     * @throws InvalidTimeSlotException if the TimeSlot is invalid
     */
    private void handleTimeSlot(Message message) throws InvalidTimeSlotException {
        TimeSlot timeSlot = (TimeSlot) message.getSendable(TimeSlot.class);
        TimeSlotValidator timeSlotValidator = new TimeSlotValidator();
        timeSlotValidator.validateTimeSlot(timeSlot, auctionManager.getTimeSlots());

        //add timeSlot to auctionManager
        auctionManager.addTimeSlots(timeSlot);
        logger.trace("Added TimeSlot: " + timeSlot);
    }

    /**
     * Handles the Sell message by validating the Sell and adding it to the sellQueue.
     *
     * @param message The Sell message to handle
     * @throws InvalidSellException if the Sell is invalid
     */
    private void handleSell(Message message) throws InvalidSellException {
        Sell sell = (Sell) message.getSendable(Sell.class);
        SellValidator sellValidator = new SellValidator();
        sellValidator.validateSell(sell);

        //add sell to queue
        sellQueue.add(sell);
        logger.trace("Added Sell: " + sell);
    }

    /**
     * Handles the Bid message by validating the Bid and adding it to the bidQueue.
     *
     * @param message The Bid message to handle
     * @throws InvalidBidException if the Bid is invalid
     */
    private void handleBid(Message message) throws InvalidBidException {
        Bid bid = (Bid) message.getSendable(Bid.class);
        BidValidator bidValidator = new BidValidator();
        bidValidator.validateBid(bid);

        //add bid to queue
        bidQueue.add(bid);
        logger.trace("Added Bid: " + bid);
    }

}
