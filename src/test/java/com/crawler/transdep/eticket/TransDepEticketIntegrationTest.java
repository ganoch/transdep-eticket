package com.crawler.transdep.eticket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for TransDepEticket crawler
 * Tests the full workflow from departures to trips
 */
public class TransDepEticketIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(TransDepEticketIntegrationTest.class);

    private TransDepEticket eticket;

    @Before
    public void setUp() throws IOException {
        logger.info("=== Setting up integration test ===");
        eticket = new TransDepEticket();
    }

    @After
    public void tearDown() throws IOException {
        logger.info("=== Cleaning up integration test ===");
        if (eticket != null) {
            eticket.close();
        }
    }

    @Test
    public void testFullWorkflow() throws IOException {
        logger.info("Test: Full Workflow (Departures -> Destinations -> Set -> Dates)");

        // Step 1: Fetch departures
        logger.info("Step 1: Fetching departures...");
        List<Map<String, String>> departures = eticket.fetchDepartures();
        assertFalse("Should have departures", departures.isEmpty());
        logger.info("✓ Found {} departures", departures.size());

        // Step 2: Set departure and fetch stops
        String departureValue = departures.get(1).get("value");
        logger.info("Step 2: Fetching stops for departure {}", departures.get(1).get("name"));
        eticket.setDeparture(departureValue);
        List<Map<String, String>> stops = eticket.fetchStops();
        if(!stops.isEmpty()){
            eticket.setStop(stops.get(stops.size() > 1 ? stops.size() - 2 : 0).get("value"));
        }
        logger.info("✓ Found {} stops", stops.size());

        // Step 3: Fetch destinations
        logger.info("Step 3: Fetching destinations...");
        List<Map<String, String>> destinations = eticket.fetchDestinations();
        assertFalse("Should have destinations", destinations.isEmpty());
        logger.info("✓ Found {} destinations", destinations.size());


        // Step 4: Set destination
        if(destinations.size() > 0){
            String destinationValue = destinations.get(0).get("value");
            destinationValue = destinations.get(destinations.size()-1).get("value");
            String destinationName = destinations.get(destinations.size()-1).get("name");

            logger.info("Step 4: Setting destination to: {}", destinationName);
            eticket.setDestination(destinationValue);
            assertEquals("Destination should be set", destinationValue, eticket.getDestination());
            logger.info("✓ Destination set");
        } else {
            logger.warn("No destinations found to set");
            return;
        }

        // Step 5: Fetch dates
        logger.info("Step 5: Fetching available dates...");
        List<Map<String, String>> trips = eticket.fetchTrips();
        if(!trips.isEmpty())
        {
            logger.info("✓ Found {} available dates", trips.size());
            trips.forEach(t -> logger.info("  - {}", t.get("name")));
        } else {
            logger.warn("No dates found for the selected departure/destination");
        }

        logger.info("✓✓✓ Full workflow completed successfully");
    }

    @Test
    public void testTripsWorkflow() throws IOException {
        logger.info("Test: Trips Workflow (includes fetching trips)");

        // Setup: departure, stop, destination, dates
        List<Map<String, String>> departures = eticket.fetchDepartures();
        String departureValue = departures.get(0).get("value");
        eticket.setDeparture(departureValue);

        List<Map<String, String>> stops = eticket.fetchStops();
        if(!stops.isEmpty()){
            eticket.setStop(stops.get(0).get("value"));
        }
        List<Map<String, String>> destinations = eticket.fetchDestinations();
        assertFalse("Should have destinations", destinations.isEmpty());
        String destinationValue = destinations.get(0).get("value");

        eticket.setDestination(destinationValue);

        logger.info("Departure: {} -> Destination: {}", departureValue, destinationValue);

        // Fetch dates
        List<Map<String, String>> trips = eticket.fetchTrips();
        if (!trips.isEmpty()) {
            String tripValue = trips.get(0).get("value");
            logger.info("Selected trip: {}", tripValue);

            logger.info("✓ Fetched {} trips", trips.size());
            trips.forEach(t -> logger.debug("  Trip: {}", t));
        }

        logger.info("✓✓✓ Trips workflow completed");
    }

    @Test
    public void testMultipleDepartureDestinationChanges() throws IOException {
        logger.info("Test: Multiple Departure/Destination Changes");

        List<Map<String, String>> departures = eticket.fetchDepartures();

        if (departures.size() >= 2) {
            // Change 1
            String dep1 = departures.get(0).get("value");
            eticket.setDeparture(dep1);
            List<Map<String, String>> stops1 = eticket.fetchStops();
            if(!stops1.isEmpty()){
                eticket.setStop(stops1.get(0).get("value"));
            }
            List<Map<String, String>> destinations1 = eticket.fetchDestinations();
            assertFalse("Should have destinations for first departure", destinations1.isEmpty());
            String dest1 = destinations1.get(0).get("value");
            eticket.setDestination(dest1);
            logger.info("Selection 1: {} -> {}", dep1, dest1);

            List<Map<String, String>> trips1 = eticket.fetchTrips();
            logger.info("✓ Found {} trips for selection 1", trips1.size());

            // Change 2
            String dep2 = departures.get(1).get("value");
            eticket.setDeparture(dep2);
            List<Map<String, String>> stops2 = eticket.fetchStops();
            if(!stops2.isEmpty()){
                eticket.setStop(stops2.get(0).get("value"));
            }
            List<Map<String, String>> destinations2 = eticket.fetchDestinations();
            assertFalse("Should have destinations for second departure", destinations2.isEmpty());
            String dest2 = destinations2.get(0).get("value");
            eticket.setDestination(dest2);
            logger.info("Selection 2: {} -> {}", dep2, dest2);

            eticket.clearCache(); // Clear cache for fresh fetch
            List<Map<String, String>> trips2 = eticket.fetchTrips();
            logger.info("✓ Found {} trips for selection 2", trips2.size());
            if(!trips2.isEmpty()){
                logger.info("  Sample trip for selection 2: {}", trips2.get(0).get("name"));
                eticket.setDispatcherId(trips2.get(0).get("value"));
            }

            logger.info("✓✓✓ Multiple changes test completed");
        } else {
            logger.warn("Not enough departures/destinations to test multiple changes");
        }
    }
}
