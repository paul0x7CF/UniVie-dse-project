package MSProsumer.Main;

import mainPackage.MainForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger logger = LogManager.getLogger(MainForTesting.class);
    public static void main(String[] args) {

        logger.info("Client Started");
        logger.debug("hello debug");
        logger.error("hello error");
        logger.fatal("hello fatal");
        logger.trace("hello trace");
        logger.warn("hello warn");
    }
}