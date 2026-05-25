package com.crawler.transdep.eticket;

import com.crawler.transdep.eticket.core.StatefulWebCrawler;
import com.crawler.transdep.eticket.parser.HtmlParser;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TransDep E-Ticket crawler for https://eticket.transdep.mn/
 *
 * Provides functionality to:
 * - Fetch available departures (stations)
 * - Fetch available destinations
 * - Set departure and destination
 * - Fetch available dates and trips
 */
public class TransDepEticket {
    private static final Logger logger = LoggerFactory.getLogger(TransDepEticket.class);

    private static final String BASE_URL = "https://eticket.transdep.mn";

    private StatefulWebCrawler crawler;
    private String departure;
    private String destination;
    private String dispatcherId;
    private CrawlerOrchestrator orchestrator;
    private Document cachedHomePage;
    private long cacheTimestamp;

    /**
     * Initialize TransDepEticket crawler
     */
    public TransDepEticket() throws IOException {
        CrawlerConfig config = new CrawlerConfig();
        config.setBaseUrl(BASE_URL);
        config.setCrawlerType("stateful");
        config.setDelayMs(500);  // Respectful delay

        this.orchestrator = new CrawlerOrchestrator(config);
        this.crawler = (StatefulWebCrawler) CrawlerFactory.createCrawler(config);

        logger.info("TransDepEticket initialized with base URL: {}", BASE_URL);
    }

    /**
     * Initialize with custom configuration
     */
    public TransDepEticket(CrawlerConfig config) throws IOException {
        if (config.getBaseUrl() == null) {
            config.setBaseUrl(BASE_URL);
        }
        config.setCrawlerType("stateful");

        this.orchestrator = new CrawlerOrchestrator(config);
        this.crawler = (StatefulWebCrawler) CrawlerFactory.createCrawler(config);

        logger.info("TransDepEticket initialized with base URL: {}", config.getBaseUrl());
    }

    /**
     * Fetch all available departures (stations)
     * @return List of departure options with name and value
     */
    public List<Map<String, String>> fetchDepartures() throws IOException {
        logger.info("Fetching departures...");
        return fetchOptions("select[name='from_location'] option, #departure option");
    }

    /**
     * Fetch all available destinations
     * @return List of destination options with name and value
     */
    public List<Map<String, String>> fetchDestinations() throws IOException {
        logger.info("Fetching destinations...");
        return fetchOptions("select[name='to_location'] option, #destination option");
    }

    /**
     * Private helper to fetch options from a single page load
     */
    private List<Map<String, String>> fetchOptions(String selector) throws IOException {
        try {
            Document page = getOrFetchHomePage();
            HtmlParser parser = new HtmlParser(page);

            List<Map<String, String>> result = new ArrayList<>();
            List<Map<String, String>> options = parser.getElementList(selector);

            for (Map<String, String> option : options) {
                if (!option.get("text").isEmpty() && !option.get("text").equalsIgnoreCase("select")) {
                    Map<String, String> item = new HashMap<>();
                    item.put("name", option.get("text"));
                    item.put("value", option.getOrDefault("value", option.get("text")));
                    result.add(item);
                }
            }

            logger.info("Found {} options for selector: {}", result.size(), selector);
            return result;

        } catch (Exception e) {
            logger.error("Error fetching options", e);
            throw new IOException("Failed to fetch options", e);
        }
    }

    /**
     * Get or fetch home page (cached for 5 seconds to avoid redundant requests)
     */
    private Document getOrFetchHomePage() throws IOException {
        long now = System.currentTimeMillis();

        // Cache for 5 seconds
        if (cachedHomePage != null && (now - cacheTimestamp) < 5000) {
            logger.debug("Using cached home page");
            return cachedHomePage;
        }

        logger.debug("Fetching fresh home page");
        cachedHomePage = crawler.getPageStateful("/");
        cacheTimestamp = now;
        return cachedHomePage;
    }

    /**
     * Clear the page cache
     */
    public void clearCache() {
        cachedHomePage = null;
        cacheTimestamp = 0;
        logger.debug("Page cache cleared");
    }

    /**
     * Set the departure station
     * @param departureValue The value of the departure station
     */
    public void setDeparture(String departureValue) throws IOException {
        logger.info("Setting departure to: {}", departureValue);

        this.departure = departureValue;

        // Submit form with departure selection
        // This may trigger AJAX to load destinations
        try {
            Document page = crawler.getPageStateful("/");
            HtmlParser parser = new HtmlParser(page);

            Map<String, String> formData = parser.parseForm("form");
            formData.put("from", departureValue);

            String formBody = buildFormBody(formData);

            // Post the selection
            Document result = crawler.postPageStateful("/", formBody);

            logger.info("Departure set to: {}", departureValue);

        } catch (Exception e) {
            logger.error("Error setting departure", e);
            throw new IOException("Failed to set departure", e);
        }
    }

    /**
     * Set the destination station
     * @param destinationValue The value of the destination station
     */
    public void setDestination(String destinationValue) throws IOException {
        logger.info("Setting destination to: {}", destinationValue);

        this.destination = destinationValue;

        // Submit form with destination selection
        try {
            Document page = crawler.getPageStateful("/");
            HtmlParser parser = new HtmlParser(page);

            Map<String, String> formData = parser.parseForm("form");
            if (departure != null) {
                formData.put("from", departure);
            }
            formData.put("to", destinationValue);

            String formBody = buildFormBody(formData);

            // Post the selection
            Document result = crawler.postPageStateful("/", formBody);

            logger.info("Destination set to: {}", destinationValue);

        } catch (Exception e) {
            logger.error("Error setting destination", e);
            throw new IOException("Failed to set destination", e);
        }
    }

    /**
     * Fetch available dates for the current departure/destination
     * Must call setDeparture() and setDestination() first
     * @return List of available dates
     */
    public List<Map<String, String>> fetchDates() throws IOException {
        if (departure == null || destination == null) {
            throw new IllegalStateException("Must call setDeparture() and setDestination() first");
        }

        logger.info("Fetching dates for {} -> {}", departure, destination);

        try {
            Document page = crawler.getPageStateful("/");
            HtmlParser parser = new HtmlParser(page);

            List<Map<String, String>> dates = new ArrayList<>();

            // Parse date options
            // Adjust selectors based on actual website structure
            List<Map<String, String>> options = parser.getElementList("select[name='date'] option, #date option");

            for (Map<String, String> option : options) {
                if (!option.get("text").isEmpty() && !option.get("text").equalsIgnoreCase("select")) {
                    Map<String, String> date = new HashMap<>();
                    date.put("name", option.get("text"));
                    date.put("value", option.getOrDefault("value", option.get("text")));
                    dates.add(date);
                }
            }

            logger.info("Found {} available dates", dates.size());
            return dates;

        } catch (Exception e) {
            logger.error("Error fetching dates", e);
            throw new IOException("Failed to fetch dates", e);
        }
    }

    /**
     * Fetch available trips for the current departure/destination and date
     * Requests: /homepage/e_ticket_seat.php?from_location=X&from_stop=&to_location=Y&dispatcher_id=Z
     * @param dateValue The date to fetch trips for (contains dispatcher_id in format "date|dispatcherId")
     * @return List of available trips with details
     */
    public List<Map<String, String>> fetchTrips(String dateValue) throws IOException {
        if (departure == null || destination == null) {
            throw new IllegalStateException("Must call setDeparture() and setDestination() first");
        }

        logger.info("Fetching trips for {} -> {} on {}", departure, destination, dateValue);

        try {
            // Extract dispatcher_id from dateValue (format: "date|dispatcherId")
            String actualDate = dateValue;
            if (dateValue.contains("|")) {
                String[] parts = dateValue.split("\\|");
                actualDate = parts[0];
                dispatcherId = parts[1];
            } else if (dispatcherId == null) {
                logger.warn("Dispatcher ID not found in dateValue: {}", dateValue);
            }

            // Build query string for e_ticket_seat.php
            String query = String.format("/homepage/e_ticket_seat.php?from_location=%s&from_stop=&to_location=%s&dispatcher_id=%s",
                    departure, destination, dispatcherId != null ? dispatcherId : "");

            logger.info("Requesting seat page: {}", query);
            Document seatPage = crawler.getPageStateful(query);
            HtmlParser parser = new HtmlParser(seatPage);

            List<Map<String, String>> trips = new ArrayList<>();

            // Parse trip/seat results from partial HTML
            // Adjust selectors based on actual website structure
            List<Map<String, String>> tripElements = parser.getElementList("div.trip-result, tr.trip-row, .trip-item, div[class*='bus'], div[class*='seat']");
            trips.addAll(tripElements);

            logger.info("Found {} trips/buses", trips.size());
            return trips;

        } catch (Exception e) {
            logger.error("Error fetching trips", e);
            throw new IOException("Failed to fetch trips", e);
        }
    }

    /**
     * Get current departure
     */
    public String getDeparture() {
        return departure;
    }

    /**
     * Get current destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Get current dispatcher ID
     */
    public String getDispatcherId() {
        return dispatcherId;
    }

    /**
     * Set dispatcher ID (usually extracted from date selection)
     */
    public void setDispatcherId(String dispatcherId) {
        this.dispatcherId = dispatcherId;
        logger.info("Dispatcher ID set to: {}", dispatcherId);
    }

    /**
     * Get the underlying crawler for advanced usage
     */
    public StatefulWebCrawler getCrawler() {
        return crawler;
    }

    /**
     * Get the orchestrator for custom step execution
     */
    public CrawlerOrchestrator getOrchestrator() {
        return orchestrator;
    }

    /**
     * Get the cached home page (for testing purposes)
     */
    public Document getCachedPage() {
        return cachedHomePage;
    }

    /**
     * Close resources
     */
    public void close() throws IOException {
        if (crawler != null) {
            crawler.close();
            logger.info("TransDepEticket crawler closed");
        }
    }

    /**
     * Build form body from map
     */
    private String buildFormBody(Map<String, String> formData) {
        StringBuilder sb = new StringBuilder();
        formData.forEach((key, value) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(key).append("=").append(urlEncode(value));
        });
        return sb.toString();
    }

    /**
     * Simple URL encoding
     */
    private String urlEncode(String value) {
        if (value == null) return "";
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
