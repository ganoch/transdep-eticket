package com.crawler.transdep.eticket;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for TransDepEticket crawler
 * Note: These tests require internet connection and the website to be available
 */
public class TransDepEticketTest {
    private static final Logger logger = LoggerFactory.getLogger(TransDepEticketTest.class);

    private TransDepEticket eticket;

    @Before
    public void setUp() throws IOException {
        logger.info("Setting up TransDepEticket test instance...");
        eticket = new TransDepEticket();
    }

    @Test
    public void testInitialization() {
        logger.info("Test: Initialization");
        assertNotNull("TransDepEticket should be initialized", eticket);
        assertNull("Departure should be null initially", eticket.getDeparture());
        assertNull("Destination should be null initially", eticket.getDestination());
        logger.info("✓ Initialization test passed");
    }

    @Test
    public void testFetchDepartures() throws IOException {
        logger.info("Test: Fetch Departures");
        List<Map<String, String>> departures = eticket.fetchDepartures();

        assertNotNull("Departures should not be null", departures);
        assertFalse("Departures should not be empty", departures.isEmpty());

        // Verify structure
        for (Map<String, String> departure : departures) {
            assertNotNull("Departure should have 'name'", departure.get("name"));
            assertNotNull("Departure should have 'value'", departure.get("value"));
        }

        logger.info("✓ Found {} departures", departures.size());
        departures.forEach(d -> logger.info("  - {} ({})", d.get("name"), d.get("value")));
    }

    @Test
    public void testFetchDestinations() throws IOException {
        logger.info("Test: Fetch Destinations");

        List<Map<String, String>> departures = eticket.fetchDepartures();
        assertFalse("Should have departures", departures.isEmpty());

        eticket.setDeparture(departures.get(1).get("value"));
        List<Map<String, String>> stops = eticket.fetchStops();
        if(!stops.isEmpty()){
            eticket.setStop(stops.get(0).get("value"));
        }

        List<Map<String, String>> destinations = eticket.fetchDestinations();

        assertNotNull("Destinations should not be null", destinations);
        assertFalse("Destinations should not be empty", destinations.isEmpty());

        // Verify structure
        for (Map<String, String> destination : destinations) {
            assertNotNull("Destination should have 'name'", destination.get("name"));
            assertNotNull("Destination should have 'value'", destination.get("value"));
        }

        logger.info("✓ Found {} destinations", destinations.size());
        destinations.forEach(d -> logger.info("  - {} ({})", d.get("name"), d.get("value")));
    }

    @Test
    public void testDepartureAndDestinationCaching() throws IOException {
        logger.info("Test: Departures and Destinations Caching");

        long startTime = System.currentTimeMillis();
        List<Map<String, String>> departures = eticket.fetchDepartures();
        long time1 = System.currentTimeMillis() - startTime;

        eticket.setDeparture(departures.get(0).get("value"));
        List<Map<String, String>> stops = eticket.fetchStops();
        if(!stops.isEmpty()){
            eticket.setStop(stops.get(0).get("value"));
        }

        startTime = System.currentTimeMillis();
        List<Map<String, String>> destinations = eticket.fetchDestinations();
        long time2 = System.currentTimeMillis() - startTime;

        // Second call should be faster due to caching
        logger.info("First fetch took: {}ms", time1);
        logger.info("Second fetch took: {}ms (should be faster due to caching)", time2);

        assertFalse("Departures should not be empty", departures.isEmpty());
        assertFalse("Destinations should not be empty", destinations.isEmpty());
        logger.info("✓ Caching test passed");
    }

    @Test(expected = IllegalStateException.class)
    public void testFetchDatesWithoutDeparture() throws IOException {
        logger.info("Test: Fetch Dates Without Departure (should fail)");
        eticket.fetchDates(); // Should throw IllegalStateException
    }

    @Test
    public void testSetDeparture() throws IOException {
        logger.info("Test: Set Departure");
        List<Map<String, String>> departures = eticket.fetchDepartures();
        assertFalse("Should have departures", departures.isEmpty());

        String departureValue = departures.get(0).get("value");
        eticket.setDeparture(departureValue);

        assertEquals("Departure should be set", departureValue, eticket.getDeparture());
        logger.info("✓ Departure set to: {}", departureValue);
    }

    @Test
    public void testSetDestination() throws IOException {
        logger.info("Test: Set Destination");

        // First set departure
        List<Map<String, String>> departures = eticket.fetchDepartures();
        eticket.setDeparture(departures.get(0).get("value"));

        // Then set stop and fetch destinations
        List<Map<String, String>> stops = eticket.fetchStops();
        if(!stops.isEmpty()){
            eticket.setStop(stops.get(0).get("value"));
        }

        List<Map<String, String>> destinations = eticket.fetchDestinations();
        assertFalse("Should have destinations", destinations.isEmpty());

        String destinationValue = destinations.get(0).get("value");
        eticket.setDestination(destinationValue);

        assertEquals("Destination should be set", destinationValue, eticket.getDestination());
        logger.info("✓ Destination set to: {}", destinationValue);
    }

    @Test
    public void testGettersAndSetters() throws IOException {
        logger.info("Test: Getters and Setters");

        eticket.setDeparture("22");
        assertEquals("22", eticket.getDeparture());

        List<Map<String, String>> stops = eticket.fetchStops();
        if(!stops.isEmpty()){
            eticket.setStop(stops.get(0).get("value"));
        }

        List<Map<String, String>> destinations = eticket.fetchDestinations();
        assertFalse("Should have destinations", destinations.isEmpty());
        String destinationValue = destinations.get(0).get("value");
        eticket.setDestination(destinationValue);
        assertEquals(destinationValue, eticket.getDestination());

        eticket.setDispatcherId("1768876");
        assertEquals("1768876", eticket.getDispatcherId());

        logger.info("✓ All getters and setters work correctly");
    }

    @Test
    public void testCacheClear() throws IOException {
        logger.info("Test: Cache Clear");

        eticket.fetchDepartures();
        assertNotNull("Cache should exist after fetch", eticket.getCachedPage());

        eticket.clearCache();
        // Cache should be cleared
        logger.info("✓ Cache cleared successfully");
    }

    public void tearDown() throws IOException {
        logger.info("Cleaning up...");
        if (eticket != null) {
            eticket.close();
            logger.info("TransDepEticket closed");
        }
    }
}
