package com.transdep.eticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating how to use the TransDepEticket crawler
 */
public class TransDepEticketExample {
    private static final Logger logger = LoggerFactory.getLogger(TransDepEticketExample.class);

    public static void main(String[] args) {
        TransDepEticket eticket = null;
        try {
            // Initialize the crawler
            logger.info("Initializing TransDep E-Ticket crawler...");
            eticket = new TransDepEticket();

            // Step 1: Fetch available departures
            logger.info("\n=== Step 1: Fetch Departures ===");
            List<Map<String, String>> departures = eticket.fetchDepartures();
            logger.info("Available departures:");
            departures.forEach(d -> logger.info("  - {} ({})", d.get("name"), d.get("value")));

            // Step 2: Fetch available destinations
            logger.info("\n=== Step 2: Fetch Destinations ===");
            List<Map<String, String>> destinations = eticket.fetchDestinations();
            logger.info("Available destinations:");
            destinations.forEach(d -> logger.info("  - {} ({})", d.get("name"), d.get("value")));

            // Step 3: Set departure
            logger.info("\n=== Step 3: Set Departure ===");
            if (!departures.isEmpty()) {
                String departureValue = departures.get(0).get("value");
                logger.info("Setting departure to: {}", departures.get(0).get("name"));
                eticket.setDeparture(departureValue);
            } else {
                logger.warn("No departures available");
                return;
            }

            // Step 4: Set destination
            logger.info("\n=== Step 4: Set Destination ===");
            if (!destinations.isEmpty()) {
                String destinationValue = destinations.get(0).get("value");
                logger.info("Setting destination to: {}", destinations.get(0).get("name"));
                eticket.setDestination(destinationValue);
            } else {
                logger.warn("No destinations available");
                return;
            }


            // Step 6: Fetch trips for the first available date
            logger.info("\n=== Step 6: Fetch Trips ===");
            List<Map<String, String>> trips = eticket.fetchTrips();
            logger.info("Available trips: {}", trips.size());
            trips.forEach(t -> logger.debug("  Trip: {}", t));

            logger.info("\n=== Crawling completed successfully ===");

        } catch (IOException e) {
            logger.error("Error during crawling", e);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        } finally {
            if (eticket != null) {
                try {
                    eticket.close();
                } catch (IOException e) {
                    logger.error("Error closing crawler", e);
                }
            }
        }
    }

    /**
     * Example showing programmatic usage
     */
    public static void demonstrateUsage() throws IOException {
        TransDepEticket eticket = new TransDepEticket();

        try {
            // Fetch options
            List<Map<String, String>> departures = eticket.fetchDepartures();
            List<Map<String, String>> destinations = eticket.fetchDestinations();

            // Set selections
            eticket.setDeparture(departures.get(0).get("value"));
            eticket.setDestination(destinations.get(0).get("value"));

            // Get trips
            List<Map<String, String>> trips = eticket.fetchTrips();

            System.out.println("Found " + trips.size() + " trips");

        } finally {
            eticket.close();
        }
    }
}
