package Communication;

import Exceptions.UnknownMessageException;
import Logic.MessageHandling.AuctionMessageHandler;
import Logic.MessageHandling.ExchangeMessageHandler;
import Logic.MessageHandling.ForecastMessageHandler;
import Logic.Prosumer.Prosumer;
import MSProsumer.Main.ProsumerManager;
import broker.BrokerRunner;
import messageHandling.MessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocol.ECategory;
import protocol.Message;
import sendable.EServiceType;
import sendable.MSData;
import sendable.TimeSlot;

import java.util.concurrent.BlockingQueue;

public class Communication {

    private static final Logger logger = LogManager.getLogger(Communication.class);

    private MSData myMSData;

    private BrokerRunner communicationBroker;
    private Prosumer myProsumer;

    private BlockingQueue<TimeSlot> inputQueueTimeSlot;

    private BlockingQueue<Message> incomingMessages;

    private BlockingQueue<Message> outgoingMessages;

    private ProsumerManager prosumerManager;

    private MessageHandler prosMessageHandler;

    public Communication(BlockingQueue<TimeSlot> inputQueueTimeSlot, BlockingQueue<Message> inputForecastResponse, BlockingQueue<Message> outgoingMessages, ProsumerManager prosumerManager) {
        this.inputQueueTimeSlot = inputQueueTimeSlot;
        //this.inputForecastResponse = inputForecastResponse;
        this.outgoingMessages = outgoingMessages;
        this.prosumerManager = prosumerManager;
    }

    public Communication(BlockingQueue<Message> incomingMessages, BlockingQueue<Message> outgoingMessages, final int port, Prosumer myProsumer) {
        this.incomingMessages = outgoingMessages;
        this.outgoingMessages = outgoingMessages;
        this.myProsumer = myProsumer;
        createBroker(port);

        logger.info("BrokerRunner initialized with Id: {} Ip: {} Port: {}", this.myMSData.getId(), this.myMSData.getAddress(), this.myMSData.getPort());
    }

    private void createBroker(final int port) {
        this.communicationBroker = new BrokerRunner(EServiceType.Prosumer, port);
        this.myMSData = this.communicationBroker.getCurrentService();
    }

    public void startBrokerRunner() {
        this.communicationBroker.run();
    }

    public void addMessageHandler(ECategory category) {
        try {
            switch (category) {
                case Auction -> {
                    this.communicationBroker.addMessageHandler(ECategory.Auction, new AuctionMessageHandler());
                }
                case Exchange -> {
                    this.communicationBroker.addMessageHandler(ECategory.Exchange, new ExchangeMessageHandler(myProsumer));
                }
                case Forecast -> {
                    this.communicationBroker.addMessageHandler(ECategory.Forecast, new ForecastMessageHandler(myProsumer));
                }
                default -> {
                    throw new UnknownMessageException();
                }
            }
        } catch (UnknownMessageException e) {
            logger.warn(e.getMessage());
        }
    }

    private void sendMessage(Message message) {

        //communicationBroker.addMessageHandler(ECategory.Auction, new AuctionMessageHandler(communicationBroker, prosumer));


    }


}
