package loadManager.networkManagment;

import loadManager.exchangeManagement.ExchangeServiceInformation;
import protocol.Message;
import protocol.MessageFactory;
import sendable.Bid;
import sendable.Sell;
import sendable.TimeSlot;

public class ExtendedMessageBuilder {
    private Message buildMessagePriceIsToLow(Bid bid, double averagePrice) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

    private Message buildMessagePriceIsToHigh(Sell sell, double averagePrice) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

    private Message buildMessageSendTimeSlot(TimeSlot timeSlot) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

    private Message buildMessageSendBid(Bid bid, ExchangeServiceInformation exchangeServiceInformation) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

    private Message buildMessageForUnsatisfiedBidder(Bid bid) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

    private Message buildMessageForUnsatisfiedSeller(Sell sell) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

    private Message buildMessageSendSell(Sell sell, ExchangeServiceInformation exchangeServiceInformation) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

    private Message buildMessageTransactionForUnsatisfiedSeller(Sell sell) {
        MessageFactory messageFactory = new MessageFactory();
        return null;
    }

}