package CF.broker;

import CF.broker.discovery.DiscoveryService;
import CF.broker.discovery.IScheduleBroker;
import CF.broker.discovery.MessageScheduler;
import CF.broker.discovery.SyncService;
import CF.communication.LocalMessage;
import CF.communication.NetworkHandler;
import CF.exceptions.MessageProcessingException;
import CF.messageHandling.InfoMessageHandler;
import CF.messageHandling.MessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import CF.protocol.ECategory;
import CF.protocol.Message;
import CF.sendable.AckInfo;
import CF.sendable.EServiceType;
import CF.sendable.MSData;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Broker implements IServiceBroker, IScheduleBroker {
    private static final Logger log = LogManager.getLogger(Broker.class);
    private final AckHandler ackHandler;
    // not IMessageHandler because addMessageHandler is not part of the interface.
    private final MessageHandler messageHandler;
    private final MessageScheduler messageScheduler;
    private final NetworkHandler networkHandler;
    private final ServiceRegistry serviceRegistry;


    protected Broker(EServiceType serviceType, int listeningPort) {
        // listeningPort is the currentMSData port so others can send messages to this broker.
        networkHandler = new NetworkHandler(serviceType, listeningPort);

        // give currentMS to registry
        serviceRegistry = new ServiceRegistry(networkHandler.getMSData());

        // AckHandler uses IBroker to send messages.
        ackHandler = new AckHandler(this);

        // MessageHandlers are based on the category so that other components can have their own implementations
        messageHandler = new MessageHandler();
        messageHandler.addMessageHandler(ECategory.Info, new InfoMessageHandler(this));

        messageScheduler = new MessageScheduler();
        messageScheduler.addObserver(new DiscoveryService(this));
        messageScheduler.addObserver(new SyncService(this));
        messageScheduler.startScheduling();
    }

    protected void startBroker() throws UnknownHostException {
        networkHandler.startSockets();

        // register Microservice
        // TODO: should we add a Info;GetAllServices message which returns a list of MSData?
        // TODO: Fixed IP addresses?
        //  Its always 10.102.102.x (Prosumer(17), Exchange(13), Forecast(9)) but whats the port?


        // start receiving messages in new thread
        try {
            receiveMessage();
        } catch (MessageProcessingException e) {
            log.error("Error while receiving message: ", e);
        }
    }

    protected void stop() throws InterruptedException {
        for (MSData service : getServices()) {
            sendMessage(InfoMessageBuilder.createUnregisterMessage(serviceRegistry.getCurrentService(), service));
        }
        Thread.sleep(1000);
        networkHandler.stop();
    }

    @Override
    public void sendMessage(Message message) {
        log.trace("{} sending message: {}", getCurrentService().getType(), message.getCategory());
        networkHandler.sendMessage(new LocalMessage(Marshaller.marshal(message),
                message.getReceiverAddress(),
                message.getReceiverPort()));

        print(message,true);
        // Ack and Register messages should not be acknowledged as this would cause an infinite loop and register
        // has an answer in the form of "Ping" anyway.
        if (!Objects.equals(message.getSubCategory(), "Ack")
                && !Objects.equals(message.getSubCategory(), "Register")) {
            ackHandler.trackMessage(message);
        }
    }

    /**
     * while true loop to receive messages and handle them
     *
     * @throws IOException                if an I/O error occurs
     * @throws ClassNotFoundException     if the class of a serialized object could not be found
     * @throws MessageProcessingException if the message could not be processed
     */
    private void receiveMessage() throws MessageProcessingException {
        // TODO: maybe catch exceptions and try again
        while (true) {
            byte[] receivedMessage = networkHandler.receiveMessage();
            if (receivedMessage == null) {
                return;
            }
            Message message = Marshaller.unmarshal(receivedMessage);
            log.trace("{} received {}", getCurrentService().getType(), message.getSubCategory());
            if (getCurrentService().getPort() == 11000) {
                log.debug("######### 11000 received {} from {}", message.getSubCategory(), message.getSenderPort());
                print(message,false);
            }
            messageHandler.handleMessage(message);

            // Ack and Register messages should not be acknowledged as this would cause an infinite loop and register
            // has an answer in the form of "Ping" anyway.
            if (!Objects.equals(message.getSubCategory(), "Ack")
                    && !Objects.equals(message.getSubCategory(), "Register")) {
                sendMessage(InfoMessageBuilder.createAckMessage(message));
            }
        }
    }

    private void print(Message message, boolean isSender) {
        int port = getCurrentService().getPort();
        if (port == 11000) { // TODO: change to unused Port
            String sr = isSender ? "S" : "R ";
            String ackBind = isSender ? "sent for" : "received for";
            String messageBind = isSender ? "message sent to" : "message received from";
            if (isSender && !message.getSubCategory().equals("Ack")) {
                log.trace("######### {} {} sent to {} | {}", port, message.getSubCategory(), message.getReceiverPort(),
                        message.getMessageID().toString().substring(0, 4));
            } else {
                log.trace("######### {} received from {} | {}", port, message.getSenderPort(), message.getMessageID());
            }
            int from = isSender ? message.getSenderPort() : message.getReceiverPort();
            if (message.getSubCategory().equals("Ack")) {
                AckInfo ackInfo = (AckInfo) message.getSendable(AckInfo.class);
                log.trace("######### "+sr+port+" {} {} {} "+sr+" {}", message.getSubCategory(), ackBind,
                        ackInfo.getMessageID().toString().substring(0, 4), from);
            } else {
                log.trace("######### "+sr+port+" {} {} {} | {}", message.getSubCategory(), messageBind, from, message.getMessageID().toString().substring(0, 4));
            }
        } else if (port == 9000) {
            log.trace("9000");
        }
    }

    @Override
    public void ackReceived(AckInfo ack) {
        ackHandler.ackReceived(ack);
    }

    @Override
    public boolean registerService(MSData msData) {
        return serviceRegistry.registerService(msData);
    }

    public void unregisterService(MSData msData) {
        serviceRegistry.unregisterService(msData);
    }

    @Override
    public MSData getCurrentService() {
        return serviceRegistry.getCurrentService();
    }

    protected MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public MSData findService(UUID serviceId) {
        return serviceRegistry.findService(serviceId);
    }

    public boolean serviceExists(int port) {
        return serviceRegistry.serviceExists(port);
    }

    public List<MSData> getServices() {
        return serviceRegistry.getAvailableServices();
    }

    public List<MSData> getServicesByType(EServiceType serviceType) {
        return serviceRegistry.getServicesByType(serviceType);
    }
}
