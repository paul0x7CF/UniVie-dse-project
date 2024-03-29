package loadManager.prosumerActionManagement.priceCalculationStrategy;

import MSP.Exceptions.PriceNotOKException;
import loadManager.prosumerActionManagement.EAction;
import mainPackage.PropertyFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeightedMechanism implements IPriceMechanism {
    private static final Logger logger = LogManager.getLogger(WeightedMechanism.class);
    private final int K_VALUES;
    private double weightedPrice = 0.0;
    private List<Double> bidPrices = new ArrayList<>();
    private List<Double> askPrices = new ArrayList<>();

    public WeightedMechanism() {
        logger.info("LOAD_MANAGER: Using the WeightedMechanism");
        PropertyFileReader propertyFileReader = new PropertyFileReader();
        K_VALUES = Integer.parseInt(propertyFileReader.getK());
    }

    public boolean isBidPriceHighEnough(double price) throws PriceNotOKException {
        if (price <= 0.0) {
            throw new PriceNotOKException("LOAD_MANAGER: Price can't be zero or lower");
        }

        if (weightedPrice == 0.0) {
            weightedPrice = price;
            updateList(price, EAction.Bid);
            return true;
        }

        if (price >= weightedPrice) {
            updateList(price, EAction.Bid);
            return true;
        }
        return false;
    }

    public boolean isAskPriceLowEnough(double price) throws PriceNotOKException {
        if (price <= 0.0) {
            throw new PriceNotOKException("LOAD_MANAGER: Price can't be zero or lower");
        }

        if (weightedPrice == 0.0) {
            weightedPrice = price;
            updateList(price, EAction.Sell);
            return true;
        }

        if (price <= weightedPrice) {
            updateList(price, EAction.Sell);
            return true;
        }
        return false;
    }

    public double getWPrice() {
        return weightedPrice;
    }

    private void calculatePrice() {
        int k = Math.min(bidPrices.size(), askPrices.size());
        if (k == 0) {
            return;
        }

        List<Double> sortedBidPrices = new ArrayList<>(bidPrices);
        List<Double> sortedAskPrices = new ArrayList<>(askPrices);
        Collections.sort(sortedBidPrices);
        Collections.sort(sortedAskPrices);

        // Weigh the bid and ask prices based on their positions in the sorted lists
        double bidSum = 0.0;
        double askSum = 0.0;
        for (int i = 0; i < k; i++) {
            double bidWeight = (i + 1) / (double) k;
            double askWeight = (k - i) / (double) k;
            bidSum += sortedBidPrices.get(i) * bidWeight;
            askSum += sortedAskPrices.get(i) * askWeight;
        }

        weightedPrice = (bidSum + askSum) / (2 * k);

        logger.debug("LOAD_MANAGER: Weighted price is {}", weightedPrice);
    }

    private void updateList(double price, EAction action) {
        switch (action) {
            case Bid -> bidPrices.add(price);
            case Sell -> askPrices.add(price);
        }

        if (bidPrices.size() > K_VALUES) {
            bidPrices.remove(0);
        }
        if (askPrices.size() > K_VALUES) {
            askPrices.remove(0);
        }

        calculatePrice();
    }
}
