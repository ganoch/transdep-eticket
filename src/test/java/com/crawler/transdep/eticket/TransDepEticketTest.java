package com.crawler.transdep.eticket;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.crawler.transdep.eticket.core.StatefulWebCrawler;

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
        eticket.fetchTrips(); // Should throw IllegalStateException
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

    @Test
    public void testFetchSeatsParsing() throws Exception {
        logger.info("Test: Fetch Seats Parsing");

        final String html = "<script language=\"javascript\">"
            +"document.getElementById(\"child_price\").value=\"22500\";"
            +"document.getElementById(\"adult_price\").value=\"45000\";"
            +"document.getElementById(\"ic_name\").innerHTML=\"Нэйшнл эженси\";"
            +"document.getElementById(\"adult_insurance_price\").value=\"1600\";"
            +"document.getElementById(\"child_insurance_price\").value=\"800\";"
            +"document.getElementById(\"os_child_price\").innerHTML=\"0\";"
            +"document.getElementById(\"os_adult_price\").innerHTML=\"0\";"
            +"</script>"
            +"<style>"
            +".seatBus{"
            +"background:url(pg/wallpaper/bus_middle.png) repeat-x;"
            +"}"
            +".seatBus td{"
            +"margin:0px;"
            +"padding:0px;"
            +"font-size:10px;"
            +"}"
            +".seatMidBus{"
            +"background:url(pg/wallpaper/mid_bus.png) no-repeat;"
            +"}"
            +"</style>"
            +"<div class=\"uk-grid uk-grid-collapse\">"
            +"<div class=\"uk-width-1-1@s uk-width-1-3@m uk-width-1-3@l uk-grid uk-grid-collapse\" style=\"text-align: left;\">"
            +"<div class=\"uk-width-1-1@m uk-width-2-5@l\">"
            +"Чиглэл:		</div>"
            +"<div class=\"uk-width-1-1@m uk-width-3-5@l\">"
            +"<strong>Ар.Эрдэнэбулган - УБ - Ар.Эрдэнэбулган</strong>"
            +"</div>"
            +"<div class=\"uk-width-1-1@m uk-width-2-5@l\">"
            +"Хөдлөх огноо:		</div>"
            +"<div class=\"uk-width-1-1@m uk-width-3-5@l\">"
            +"<strong>2026-06-06 08:00</strong>"
            +"</div>"
            +"<div class=\"uk-width-1-1\">"
            +"</div>"
            +"<div class=\"uk-width-1-1@m uk-width-2-5@l\">"
            +"ААН нэр:		</div>"
            +"<div class=\"uk-width-1-1@m uk-width-3-5@l\">"
            +"<strong>Шуудан Тээх ХХК</strong>"
            +"</div>"
            +"<div class=\"uk-width-1-1@m uk-width-2-5@l\">"
            +"Марк загвар:		</div>"
            +"<div class=\"uk-width-1-1@m uk-width-3-5@l\">"
            +"<strong>Universe-45</strong>"
            +"</div>"
            +"<div class=\"uk-width-1-1@m uk-width-2-5@l\">"
            +"Улсын дугаар:		</div>"
            +"<div class=\"uk-width-1-1@m uk-width-3-5@l\">"
            +"<strong>75-90 УЕН</strong>"
            +"</div>"
            +"</div>"
            +"<div class=\"uk-width-1-1@s uk-width-2-3@m uk-width-2-3@l\">"
            +"<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"seatBus\" align=\"center\">"
            +"<tr>"
            +"<td rowspan=\"7\" style=\"background:url(pg/wallpaper/bus_front.png) no-repeat; width:88px; height:150px;\"></td>"
            +"<td colspan=\"33\" height=\"8\"></td>"
            +"<td rowspan=\"7\" style=\"background:url(pg/wallpaper/bus_rear.png) no-repeat; width:30px; height:150px;\"></td>"
            +"</tr>"
            +"<tr>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td colspan=\"3\"></td>"
            +"<td width=\"10\">&nbsp;</td>"
            +"<td align=\"right\"><div style=\"\">43</div></td>"
            +"<td><div style=\"\"><input type=\"checkbox\" id=\"seat_number43\" onchange=\"checkSeats()\" title=\"43\"   /></div></td>"
            +"</tr>"
            +"<tr>"
            +"<td width=\"10\">&nbsp;</td>"
            +"<td align=\"right\"><div style=\"background:#FF0\">2</div></td>"
            +"<td><div style=\"background:#FF0\"><input type=\"checkbox\" id=\"seat_number2\" onchange=\"checkSeats()\" title=\"2\"   /></div></td>"
            +"<td width=\"10\">&nbsp;</td>"
            +"<td align=\"right\"><div style=\"\">6</div></td>"
            +"<td><div style=\"\"><input type=\"checkbox\" id=\"seat_number6\" onchange=\"checkSeats()\" title=\"6\" disabled checked /></div></td>"
            +"<td width=\"10\">&nbsp;</td>"
            +"</tr>"
            +"<tr>"
            +"<td colspan=\"33\" height=\"8\"></td>"
            +"</tr>"
            +"</table>"
            +"<div class=\"uk-width-1-1\" style=\"text-align:center; font-size:11px; height: 10px;\">"
            +"Чагтлагдаагүй суудлуудаас сонгоно уу. "
            +"<span style='background:#FF0;'><input type='checkbox'></span> "
            +"- Хөгжлийн бэрхшээлтэй иргэдэд зориулсан суудал"
            +"</div>"
            +"</div>"
            +"</div>";

        // Stub crawler that returns our HTML for any seat page request
        StatefulWebCrawler stubCrawler = new StatefulWebCrawler("https://eticket.transdep.mn") {
            @Override
            public Document getPageStateful(String path) throws IOException {
                return Jsoup.parse(html, "https://eticket.transdep.mn");
            }

            @Override
            public void crawl() throws IOException {
                // no-op for test
            }
        };

        // Inject stub crawler into eticket instance via reflection
        Field crawlerField = TransDepEticket.class.getDeclaredField("crawler");
        crawlerField.setAccessible(true);
        crawlerField.set(eticket, stubCrawler);

        // Prepare state
        eticket.setDeparture("1");
        eticket.setStop("12");
        eticket.setDestination("215");
        eticket.setDispatcherId("1780135");

        List<Map<String, String>> seats = eticket.fetchSeats();
        assertNotNull("Seats should not be null", seats);
        assertEquals("Should parse three seats", 3, seats.size());

        Map<String, String> map = new HashMap<>();
        for (Map<String, String> s : seats) {
            map.put(s.get("id"), s.get("status"));
        }

        assertEquals("available", map.get("seat_number43"));
        assertEquals("accessible", map.get("seat_number2"));
        assertEquals("unavailable", map.get("seat_number6"));

        Map<String, String> metadata = eticket.getSeatPageMetadata();
        assertNotNull("Seat page metadata should not be null", metadata);
        assertEquals("22500", metadata.get("child_price"));
        assertEquals("45000", metadata.get("adult_price"));
        assertEquals("Нэйшнл эженси", metadata.get("ic_name"));
        assertEquals("1600", metadata.get("adult_insurance_price"));
        assertEquals("800", metadata.get("child_insurance_price"));
        assertEquals("0", metadata.get("os_child_price"));
        assertEquals("0", metadata.get("os_adult_price"));

        logger.info("✓ Seat parsing test passed");
    }

    public void tearDown() throws IOException {
        logger.info("Cleaning up...");
        if (eticket != null) {
            eticket.close();
            logger.info("TransDepEticket closed");
        }
    }
}
