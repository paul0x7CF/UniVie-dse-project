package CF.broker;

import CF.exceptions.AckTimeoutException;
import CF.mainPackage.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import CF.protocol.Message;
import CF.sendable.AckInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * AckHandler class that is used to track messages and resend them if no ack is received within the timeout. It is
 * used by the Broker to track messages that are sent to other services. If no ack is received within the timeout from
 * the {@link ConfigReader}, the message is resent. If an ack is received, the message is removed from the tracker. If
 * an ack is received for a message that is not tracked, a warning is logged.
 */
public class AckHandler {
    private static final Logger log = LogManager.getLogger(AckHandler.class);

    private final int timeout; // seconds to wait for ack before resending
    private final Map<UUID, Message> pendingAcks;
    private final ScheduledExecutorService scheduler;
    private final IBroker broker;

    public AckHandler(IBroker broker) {
        this.broker = broker;
        this.pendingAcks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(100);

        ConfigReader configReader = new ConfigReader();
        this.timeout = Integer.parseInt(configReader.getProperty("ackTimeout"));
    }

    /**
     * Tracks a message and schedules a task to resend it if no ack is received within the timeout.
     *
     * @param message The message to track.
     */
    public void trackMessage(Message message) {
        UUID messageId = message.getMessageID();
        log.trace("Tracking {} message from: {} to {} mid: {}",
                message.getSubCategory(), message.getSenderPort(), message.getReceiverPort(), message.getMessageID());
        pendingAcks.put(messageId, message);

        scheduler.schedule(() -> {
            if (pendingAcks.remove(messageId) != null) {
                try {
                    log.debug("{}: No Ack received, resending {} from {} to {} | {}", message.getSenderPort(),
                            message.getSubCategory(), message.getSenderPort(), message.getReceiverPort(), message.getMessageID().toString().substring(0, 4));
                    throw new AckTimeoutException("Message could not be acknowledged within the given timeout");
                } catch (AckTimeoutException e) {
                    // don't send an infinite amount of messages - scheduler only executes this once
                    broker.sendMessage(message);
                    throw new RuntimeException(e);
                }
            }
        }, timeout, TimeUnit.SECONDS);
    }

    /**
     * Removes a message from the tracker if an ack is received.
     *
     * @param ack The ack to remove.
     */
    public void ackReceived(AckInfo ack) {
        if (!pendingAcks.containsKey(ack.getMessageID())) {
            pendingAcks.remove(ack.getMessageID());
            log.warn("{}: ack from {} for unknown {} message | {}", ack.getReceiverPort(),
                    ack.getSenderPort(), ack.getCategory(), ack.getMessageID().toString().substring(0, 4));
            return;
        } else {
            log.trace("{}: Removing ack from {} for {} message | {}", ack.getReceiverPort(),
                    ack.getSenderPort(), ack.getCategory(), ack.getMessageID().toString().substring(0, 4));
            if (pendingAcks.remove(ack.getMessageID()) == null) {
                log.warn("{}: Unknown message from {} for {} message | {}", ack.getReceiverPort(),
                        ack.getSenderPort(), ack.getCategory(), ack.getMessageID());
                return;
            }
        }

        log.trace("Received ack for message {}", ack.getMessageID());
    }
}