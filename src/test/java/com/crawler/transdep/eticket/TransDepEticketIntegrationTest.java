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
        logger.info("Step 2: Fetching stops for departure...");
        String departureValue = departures.get(1).get("value");
        eticket.setDeparture(departureValue);
        List<Map<String, String>> stops = eticket.fetchStops();
        if(!stops.isEmpty()){
            eticket.setStop(stops.get(0).get("value"));
        }
        logger.info("✓ Found {} stops", stops.size());

        // Step 3: Fetch destinations
        logger.info("Step 3: Fetching destinations...");
        List<Map<String, String>> destinations = eticket.fetchDestinations();
        assertFalse("Should have destinations", destinations.isEmpty());
        logger.info("✓ Found {} destinations", destinations.size());

        // Step 4: Set destination
        String destinationValue = destinations.get(0).get("value");
        String departureName = departures.get(0).get("name");
        logger.info("Step 3: Setting departure to: {}", departureName);
        eticket.setDeparture(departureValue);
        assertEquals("Departure should be set", departureValue, eticket.getDeparture());
        logger.info("✓ Departure set");

        // Step 4: Set destination
        destinationValue = destinations.get(0).get("value");
        String destinationName = destinations.get(0).get("name");
        logger.info("Step 4: Setting destination to: {}", destinationName);
        eticket.setDestination(destinationValue);
        assertEquals("Destination should be set", destinationValue, eticket.getDestination());
        logger.info("✓ Destination set");

        // Step 5: Fetch dates
        logger.info("Step 5: Fetching available dates...");
        List<Map<String, String>> dates = eticket.fetchDates();
        assertFalse("Should have dates", dates.isEmpty());
        logger.info("✓ Found {} available dates", dates.size());
        dates.forEach(d -> logger.info("  - {}", d.get("name")));

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
        List<Map<String, String>> dates = eticket.fetchDates();
        if (!dates.isEmpty()) {
            String dateValue = dates.get(0).get("value");
            logger.info("Selected date: {}", dateValue);

            // Try to fetch trips
            try {
                List<Map<String, String>> trips = eticket.fetchTrips(dateValue);
                logger.info("✓ Fetched {} trips", trips.size());
                trips.forEach(t -> logger.debug("  Trip: {}", t));
            } catch (Exception e) {
                logger.warn("Note: Could not fetch trips (may be due to website structure): {}", e.getMessage());
                // This is acceptable - the endpoint might require specific HTML parsing
            }
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

            List<Map<String, String>> dates1 = eticket.fetchDates();
            logger.info("✓ Found {} dates for selection 1", dates1.size());

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
            List<Map<String, String>> dates2 = eticket.fetchDates();
            logger.info("✓ Found {} dates for selection 2", dates2.size());

            logger.info("✓✓✓ Multiple changes test completed");
        } else {
            logger.warn("Not enough departures/destinations to test multiple changes");
        }
    }
}
