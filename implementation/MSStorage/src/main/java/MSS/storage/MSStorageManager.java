package MSS.storage;

import CF.sendable.EServiceType;
import CF.sendable.Transaction;
import MSS.communication.Communication;
import MSS.data.Wallet;
import MSS.dataBase.TransactionDAO;
import MSS.exceptions.StorageEmptyException;
import MSS.exceptions.StorageExiredException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The MSStorageManager class represents a storage Building for an energy storage facility composed of storage cells.
 * It manages the creation, addition, and removal of energy from the storage cells.
 */
public class MSStorageManager implements Runnable {


    private static final Logger logger = LogManager.getLogger(MSStorageManager.class);

    private final LinkedHashMap<Integer, StorageCell> storageCells = new LinkedHashMap<>();
    private BlockingQueue<Transaction> incomingTransactionQueue;
    private Wallet wallet;
    private CallbackStorageCellTerminated callbackTermination;
    private Communication communicator;

    private TransactionDAO transactionDAO;


    /**
     * Constructs an instance of the MSStorageManager class.
     * It initializes the callbackTermination object.
     */
    public MSStorageManager(final int port) {
        this.callbackTermination = actOnCallback();
        this.transactionDAO = new TransactionDAO();
        this.transactionDAO.deleteAll();
        this.communicator = new Communication(this.incomingTransactionQueue, port, EServiceType.Storage);
        this.incomingTransactionQueue = new LinkedBlockingQueue<>();

    }

    /**
     * Creates a new storage cell with the specified period and maximum volume.
     * The created storage cell is added to the storageCells map and started in a new thread.
     *
     * @param period     The period for the storage cell.
     * @param maxVolume  The maximum volume for the storage cell.
     */
    private void createStorageCell(Duration period, double maxVolume) {
        int nextStorageCellID = this.storageCells.size() + 1;
        StorageCell storageCellToStart = new StorageCell(period, maxVolume, this.callbackTermination, nextStorageCellID);
        this.storageCells.put(nextStorageCellID, storageCellToStart);
        new Thread(storageCellToStart, "Storage-Cell-" + nextStorageCellID).start();
    }

    /**
     * Creates a storage cell with default values (period of 10 seconds and maximum volume of 10).
     * The created storage cell is added to the storageCells map and started in a new thread.
     */
    private void createStorageCell() {
        createStorageCell(Duration.ofSeconds(10), 10);
    }


    /**
     * Creates a callback implementation for storage cell termination.
     * The callback logs a warning message and removes the terminated storage cell from the storageCells map.
     *
     * @return The callback implementation for storage cell termination.
     */
    private CallbackStorageCellTerminated actOnCallback() {
        return storageCellID -> {
            logger.warn("callback from Storage Cell about termination with Id {}", storageCellID);
            this.storageCells.remove(storageCellID);
        };
    }

    /**
     * Increases the volume of the storage cells by the specified amount.
     * The volume is added to the storage cells in a sequential manner, from the first storage cell to the last.
     * If there is not enough capacity in the existing storage cells, a new storage cell is created and the process is repeated.
     *
     * @param volumeToAdd The volume to add to the storage cells.
     * @throws StorageExiredException If there is not enough capacity in the storage cells to add the specified volume.
     */
    private void increaseStorageCellVolume(double volumeToAdd) {
        double addedResult = 0;
        for (var entry : this.storageCells.entrySet()) {
            StorageCell currStorageCell = entry.getValue();
            addedResult = currStorageCell.increaseVolume(volumeToAdd);
            if (addedResult == 0) {
                break;
            }
        }
        if (addedResult != 0) {
            try {
                throw new StorageExiredException("Not enough StorageCell available to adding the asked volume of " + volumeToAdd);
            } catch (StorageExiredException e) {
                logger.warn(e.getMessage()+"; creating new one");
                increaseStorageCellVolume(volumeToAdd);
            }
        }
    }

    /**
     * Decrements the volume of the storage cells by the specified amount.
     * The volume is decremented from the storage cells in a sequential manner, from the last storage cell to the first.
     * If there is not enough volume in the existing storage cells, a StorageEmptyException is thrown.
     * In that case, a new storage cell is created and the process is repeated.
     *
     * @param volumeToDecrement The volume to decrement from the storage cells.
     * @throws StorageEmptyException If there is not enough volume in the storage cells to decrement the specified amount.
     */
    private void decrementStorageCellVolume(double volumeToDecrement) {
        double result = volumeToDecrement;
        // Durchlaufe die LinkedHashMap von hinten nach vorne mit einem Iterator
        ListIterator<Map.Entry<Integer, StorageCell>> iterator = new ArrayList<>(this.storageCells.entrySet()).listIterator(this.storageCells.size());
        while (iterator.hasPrevious()) {
            Map.Entry<Integer, StorageCell> entry = iterator.previous();
            Integer key = entry.getKey();
            StorageCell value = entry.getValue();
            result = value.decrementVolume(result);
            if (result == 0) {
                break;
            }
        }
        if (result != 0) {
            double decrementedVolume = volumeToDecrement - result;
            try {
                throw new StorageEmptyException("Not enough volume in StorageCells to decrement " + volumeToDecrement + " volume " + decrementedVolume + " where decremented " + result + " remaining");
            } catch (StorageEmptyException e) {
                logger.warn(e.getMessage()+"; creating new StorageCell");
                createStorageCell();
                decrementStorageCellVolume(result);
            }

        }

    }


    @Override
    public void run() {
        communicator.startBrokerRunner(Thread.currentThread().getName());
       while (true)
       {
           try {
               Transaction newTransaction = this.incomingTransactionQueue.take();

           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }


       }


    }

    public void handleFinishedTransaction(Transaction finishedTransaction) {

    }

    private Transaction saveFinishedTransaction(Transaction transactionToAdd) {
        return null;
    }
}
