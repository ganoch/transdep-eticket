package com.transdep.eticket;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.transdep.eticket.core.StatefulWebCrawler;
import com.transdep.eticket.parser.HtmlParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String stop;
    private String destination;
    private String dispatcherId;
    private CrawlerOrchestrator orchestrator;
    private Document cachedHomePage;
    private Document dispatcherPage;
    private Map<String, String> seatPageMetadata;
    private Map<String, BigDecimal> seatPagePricing;
    private long cacheTimestamp;

    private static final long SESSION_DURATION_MS = 5 * 60 * 1000;
    private TransDepSession session;

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
        return fetchOptions("select[name='from_location'] option");
    }

    /**
     * Fetch all available destinations
     * @return List of destination options with name and value
     */
    public List<Map<String, String>> fetchDestinations() throws IOException {
        if (departure == null) {
            throw new IllegalStateException("Must call setDeparture() first");
        }
        validateSession("fetchDestinations");
        // Some departures don't have an intermediate "stop"; when that's the case
        // the page's `selectFromLocation` logic populates `to_location` based on
        // the selected `from_location` (departure).

        logger.info("Fetching destinations for stop {}", stop);
        return parseToLocationOptions(stop);
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
                String text = option.get("text").trim();
                if (option.containsKey("disabled")) {
                    continue;
                }
                if (!text.isEmpty()
                        && !text.equalsIgnoreCase("select")
                        && !text.equalsIgnoreCase("сонгох")) {
                    Map<String, String> item = new HashMap<>();
                    item.put("name", text);
                    item.put("value", option.getOrDefault("value", text));
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

        // If the existing session expired, reset everything and start fresh.
        if (isSessionExpired(now)) {
            logger.info("Session expired after 5 minutes, starting a fresh session from home");
            clearSession();
        }

        // Cache for 5 seconds
        if (cachedHomePage != null && (now - cacheTimestamp) < 5000) {
            logger.debug("Using cached home page");
            return cachedHomePage;
        }

        logger.debug("Fetching fresh home page");
        cachedHomePage = crawler.getPageStateful("/");
        cacheTimestamp = now;
        startSession(now);
        return cachedHomePage;
    }

    /**
     * Clear the page cache
     */
    public void clearCache() {
        cachedHomePage = null;
        dispatcherPage = null;
        seatPageMetadata = null;
        seatPagePricing = null;
        stop = null;
        cacheTimestamp = 0;
        logger.debug("Page cache cleared");
    }

    private void startSession(long now) {
        session = new TransDepSession(crawler.getCookieJar(), SESSION_DURATION_MS);
        session.startSession(now);
        session.setDeparture(departure);
        session.setStop(stop);
        session.setDestination(destination);
        session.setDispatcherId(dispatcherId);
        logger.debug("Session started at {} with sessionId={}", now, session.getSessionId());
    }

    private void clearSession() {
        if (session != null) {
            session.clear();
        }
        session = null;
        cachedHomePage = null;
        dispatcherPage = null;
        seatPageMetadata = null;
        seatPagePricing = null;
        departure = null;
        stop = null;
        destination = null;
        dispatcherId = null;
        cacheTimestamp = 0;
        logger.debug("Session cleared");
    }

    private boolean isSessionExpired(long now) {
        return session != null && session.isExpired(now);
    }

    private void validateSession(String action) {
        long now = System.currentTimeMillis();
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("No active session. Start from the home page before calling " + action + ".");
        }
        if (session.isExpired(now)) {
            clearSession();
            throw new IllegalStateException("Session expired after 5 minutes. Refresh from the home page and re-select values.");
        }
        session.touch(now);
    }

    private boolean hasRouteSelection() {
        return departure != null && destination != null;
    }

    private void clearDispatcherPage() {
        dispatcherPage = null;
    }

    private void loadDispatcherPage() throws IOException {
        validateSession("loadDispatcherPage");
        if (!hasRouteSelection()) {
            throw new IllegalStateException("Departure, stop, and destination must all be set before loading dispatcher data");
        }

        String query = String.format("/homepage/e_dispatcher.php?from_location=%s&from_stop=%s&to_location=%s&dispatcher=&type=2",
                urlEncode(departure), urlEncode(stop), urlEncode(destination));

        logger.info("Loading dispatcher select page: {}", query);
        dispatcherPage = crawler.getPageStateful(query, true);
    }

    /**
     * Set the departure station
     * @param departureValue The value of the departure station
     */
    public void setDeparture(String departureValue) throws IOException {
        logger.info("Setting departure to: {}", departureValue);

        getOrFetchHomePage();

        this.departure = departureValue;
        this.stop = null;
        this.destination = null;
        this.dispatcherId = null;
        clearDispatcherPage();
        if (session != null) {
            session.setDeparture(departureValue);
            session.setStop(null);
            session.setDestination(null);
            session.setDispatcherId(null);
        }

        logger.info("Departure set to: {}", departureValue);
    }

    /**
     * Fetch stops for the current departure
     * @return List of stop options with name and value
     */
    public List<Map<String, String>> fetchStops() throws IOException {
        if (departure == null) {
            throw new IllegalStateException("Must call setDeparture() first");
        }
        validateSession("fetchStops");

        logger.info("Fetching stops for departure {}", departure);
        return parseStopOptions(departure);
    }

    /**
     * Set the from_stop value used to build destination options and dispatcher query
     * @param stopValue The value of the selected stop
     */
    public void setStop(String stopValue) throws IOException {
        logger.info("Setting stop to: {}", stopValue);

        validateSession("setStop");
        if (departure == null) {
            throw new IllegalStateException("Must call setDeparture() before setStop()");
        }

        this.stop = stopValue;
        this.destination = null;
        this.dispatcherId = null;
        clearDispatcherPage();
        if (session != null) {
            session.setStop(stopValue);
            session.setDestination(null);
            session.setDispatcherId(null);
        }

        logger.info("Stop set to: {}", stopValue);
    }

    private List<Map<String, String>> parseStopOptions(String departureValue) throws IOException {
        Document page = getOrFetchHomePage();
        String html = page.html();

        String ifBlock = extractIfBlockContent(html, departureValue);
        if (ifBlock == null) {
            logger.info("No stop options found for departure {} - returning empty list", departureValue);
            return new ArrayList<>();
        }

        Pattern pattern = Pattern.compile("\\$\\(\"#from_stop\"\\)\\.html\\(\"([\\s\\S]*?)\"\\);", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(ifBlock);
        if (!matcher.find()) {
            logger.info("No from_stop HTML found for departure {} in matched block", departureValue);
            return new ArrayList<>();
        }

        String optionHtml = matcher.group(1).replace("\\\"", "\"").replace("\\n", "").replace("\\r", "");
        Document fragment = Jsoup.parseBodyFragment("<select>" + optionHtml + "</select>");
        HtmlParser parser = new HtmlParser(fragment);

        List<Map<String, String>> options = new ArrayList<>();
        for (Map<String, String> option : parser.getElementList("option")) {
            String text = option.get("text").trim();
            if (option.containsKey("disabled")) {
                continue;
            }
            if (!text.isEmpty()
                    && !text.equalsIgnoreCase("select")
                    && !text.equalsIgnoreCase("сонгох")) {
                Map<String, String> item = new HashMap<>();
                item.put("name", text);
                item.put("value", option.getOrDefault("value", text));
                options.add(item);
            }
        }

        logger.info("stop options: {}", options.toString());

        return options;
    }

    private String extractIfBlockContent(String html, String departureValue) {
        String[] conditions = new String[] {
                "if\\s*\\(\\s*" + Pattern.quote(departureValue) + "\\s*==\\s*val\\s*\\)",
                "if\\s*\\(\\s*val\\s*==\\s*" + Pattern.quote(departureValue) + "\\s*\\)"
        };

        for (String condition : conditions) {
            Matcher matcher = Pattern.compile(condition).matcher(html);
            while (matcher.find()) {
                int braceStart = html.indexOf('{', matcher.end());
                if (braceStart == -1) {
                    continue;
                }
                int depth = 1;
                boolean inSingleQuote = false;
                boolean inDoubleQuote = false;
                boolean escape = false;
                for (int i = braceStart + 1; i < html.length(); i++) {
                    char c = html.charAt(i);
                    if (escape) {
                        escape = false;
                        continue;
                    }
                    if (c == '\\') {
                        escape = true;
                        continue;
                    }
                    if (!inSingleQuote && c == '"') {
                        inDoubleQuote = !inDoubleQuote;
                        continue;
                    }
                    if (!inDoubleQuote && c == '\'') {
                        inSingleQuote = !inSingleQuote;
                        continue;
                    }
                    if (inSingleQuote || inDoubleQuote) {
                        continue;
                    }
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            return html.substring(braceStart + 1, i);
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<Map<String, String>> parseToLocationOptions(String stopValue) throws IOException {
        Document page = getOrFetchHomePage();
        String html = page.html();

        logger.debug("Parsing to_location options for stop {}", stopValue);
        // If stopValue is null, some departures populate #to_location directly
        // without a stop key. In that case, match the first occurrence of
        // $("#to_location").html("...") and parse its options.
        if (stopValue == null) {
            Pattern toLocationHtmlPattern = Pattern.compile("\\$\\(\\s*\"#to_location\"\\s*\\)\\.html\\(\\s*\"([\\s\\S]*?)\"\\s*\\)\\s*;", Pattern.DOTALL);
            Matcher htmlMatcher = toLocationHtmlPattern.matcher(html);
            if (htmlMatcher.find()) {
                String optionHtmlRaw = htmlMatcher.group(1);
                String optionHtml = optionHtmlRaw.replace("\\\"", "\"").replace("\\n", "").replace("\\r", "");
                Document fragment = Jsoup.parseBodyFragment("<select>" + optionHtml + "</select>");
                HtmlParser parser = new HtmlParser(fragment);

                List<Map<String, String>> options = new ArrayList<>();
                for (Map<String, String> option : parser.getElementList("option")) {
                    String text = option.get("text").trim();
                    if (option.containsKey("disabled")) {
                        continue;
                    }
                    if (!text.isEmpty() && !text.equalsIgnoreCase("select") && !text.equalsIgnoreCase("сонгох")) {
                        Map<String, String> item = new HashMap<>();
                        item.put("name", text);
                        item.put("value", option.getOrDefault("value", text));
                        options.add(item);
                    }
                }
                logger.info("Parsed {} to_location options from inline HTML (no stop key)", options.size());
                return options;
            } else {
                logger.warn("No inline #to_location HTML found when stopValue is null");
                return new ArrayList<>();
            }
        }

        Pattern locationsPattern = Pattern.compile("const\\s+TO_LOCATIONS\\s*=\\s*\\{([\\s\\S]*?)\\};", Pattern.DOTALL);
        Matcher locationsMatcher = locationsPattern.matcher(html);
        if (locationsMatcher.find()) {
            String locationsBody = locationsMatcher.group(1);
            Pattern stopPattern = Pattern.compile("\"" + Pattern.quote(stopValue) + "\"\\s*:\\s*\\[([\\s\\S]*?)\\](,|\\})", Pattern.DOTALL);
            Matcher stopMatcher = stopPattern.matcher(locationsBody);
            if (stopMatcher.find()) {
                String arrayBody = stopMatcher.group(1);
                Pattern itemPattern = Pattern.compile("\\{[\\s\\S]*?\"id\"\\s*:\\s*(\\d+)[\\s\\S]*?\"name\"\\s*:\\s*\"((?:\\\\\"|[^\"])*?)\"[\\s\\S]*?\\}", Pattern.DOTALL);
                Matcher itemMatcher = itemPattern.matcher(arrayBody);

                List<Map<String, String>> options = new ArrayList<>();
                while (itemMatcher.find()) {
                    String id = itemMatcher.group(1);
                    String name = itemMatcher.group(2).replace("\\\"", "\"").trim();
                    if (name.isEmpty() || name.equalsIgnoreCase("select") || name.equalsIgnoreCase("сонгох")) {
                        continue;
                    }
                    Map<String, String> item = new HashMap<>();
                    item.put("name", name);
                    item.put("value", id);
                    options.add(item);
                }

                logger.info("Parsed {} to_location options for stop {} from TO_LOCATIONS", options.size(), stopValue);
                return options;
            } else {
                logger.debug("TO_LOCATIONS present but no entry for key {}", stopValue);
            }
        } else {
            logger.debug("TO_LOCATIONS block not found in homepage HTML for stop {} - will try inline HTML", stopValue);
        }

        // Fallback: look for inline JS that sets the #to_location HTML for a specific departure
        // Strategy: find all occurrences of $("#to_location").html("..."), then pick the one whose
        // nearest preceding if(...) condition contains the desired key (e.g. 22).
        Pattern toLocationHtmlPattern = Pattern.compile("\\$\\(\\s*\"#to_location\"\\s*\\)\\.html\\(\\s*\"([\\s\\S]*?)\"\\s*\\)\\s*;", Pattern.DOTALL);
        Matcher htmlMatcher = toLocationHtmlPattern.matcher(html);
        while (htmlMatcher.find()) {
            int matchStart = htmlMatcher.start();
            String optionHtmlRaw = htmlMatcher.group(1);

            // look backwards from matchStart to find the last 'if(' before this occurrence
            int ifPos = html.lastIndexOf("if", matchStart);
            if (ifPos == -1) {
                continue;
            }
            // extract a reasonable window between 'if' and the html insertion
            int windowStart = Math.max(0, ifPos);
            String window = html.substring(windowStart, matchStart);

            // check if the window contains the key in either ordering: if(22==val) or if(val==22)
            String key = Pattern.quote(stopValue);
            Pattern cond1 = Pattern.compile("if\\s*\\(\\s*" + key + "\\s*==\\s*val", Pattern.DOTALL);
            Pattern cond2 = Pattern.compile("if\\s*\\(\\s*val\\s*==\\s*" + key, Pattern.DOTALL);
            if (cond1.matcher(window).find() || cond2.matcher(window).find()) {
                String optionHtml = optionHtmlRaw.replace("\\\"", "\"").replace("\\n", "").replace("\\r", "");
                Document fragment = Jsoup.parseBodyFragment("<select>" + optionHtml + "</select>");
                HtmlParser parser = new HtmlParser(fragment);

                List<Map<String, String>> options = new ArrayList<>();
                for (Map<String, String> option : parser.getElementList("option")) {
                    String text = option.get("text").trim();
                    if (!text.isEmpty() && !text.equalsIgnoreCase("select") && !text.equalsIgnoreCase("сонгох")) {
                        Map<String, String> item = new HashMap<>();
                        item.put("name", text);
                        item.put("value", option.getOrDefault("value", text));
                        options.add(item);
                    }
                }

                logger.info("Parsed {} to_location options for key {} from inline to_location HTML", options.size(), stopValue);
                return options;
            }
        }

        logger.warn("No to_location data found for key {} (neither TO_LOCATIONS nor inline HTML)", stopValue);
        return new ArrayList<>();
    }

    /**
     * Set the destination station
     * @param destinationValue The value of the destination station
     */
    public void setDestination(String destinationValue) throws IOException {
        logger.info("Setting destination to: {}", destinationValue);

        validateSession("setDestination");
        if (departure == null || stop == null) {
            throw new IllegalStateException("Must call setDeparture() and setStop() before setDestination()");
        }

        this.destination = destinationValue;
        if (session != null) {
            session.setDestination(destinationValue);
            session.setDispatcherId(null);
        }
        clearDispatcherPage();

        if (hasRouteSelection()) {
            loadDispatcherPage();
        }
        logger.info("Destination set to: {}", destinationValue);
    }

    /**
     * Fetch available dates for the current departure/destination
     * Must call setDeparture() and setDestination() first
     * @return List of available dates
     */
    public List<Map<String, String>> fetchTrips() throws IOException {
        if (departure == null || destination == null) {
            throw new IllegalStateException("Must call setDeparture(), and setDestination() first");
        }
        validateSession("fetchTrips");

        logger.info("Fetching dates for {} -> {}", departure, destination);

        try {
            if (dispatcherPage == null) {
                loadDispatcherPage();
            }
            HtmlParser parser = new HtmlParser(dispatcherPage);

            List<Map<String, String>> dates = new ArrayList<>();

            // Parse date options from the dispatcher response fragment
            List<Map<String, String>> options = parser.getElementList("select option, option");

            for (Map<String, String> option : options) {
                String text = option.get("text");
                if (!text.isEmpty()
                        && !text.equalsIgnoreCase("select")
                        && !text.equalsIgnoreCase("сонгох")) {
                    Map<String, String> date = new HashMap<>();
                    date.put("name", text);
                    date.put("value", option.getOrDefault("value", text));
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
     * Fetch available seats for the currently selected dispatcher/trip
     * Requires: setDeparture(), setDestination(), and setDispatcherId() (trip) to be called first
     * @return Map containing "seats", "metadata", and "pricing" from the seat page
     */
    public Map<String, Object> fetchSeatsData() throws IOException {
        if (departure == null || destination == null || dispatcherId == null) {
            throw new IllegalStateException("Must call setDeparture(), setDestination(), and setDispatcherId() first");
        }
        validateSession("fetchSeatsData");

        logger.info("Fetching seats for dispatcher {} ({} -> {})", dispatcherId, departure, destination);

        try {
            // Request the dedicated seat page for the selected dispatcher/trip
            String seatQuery = String.format("/homepage/e_ticket_seat.php?from_location=%s&from_stop=%s&to_location=%s&dispatcher_id=%s",
                    urlEncode(departure), urlEncode(stop), urlEncode(destination), urlEncode(dispatcherId));

            logger.info("Loading seat page: {}", seatQuery);
            Document seatPage = crawler.getPageStateful(seatQuery, true);
            HtmlParser parser = new HtmlParser(seatPage);
            List<Map<String, String>> seats = new ArrayList<>();

            // Prefer precise parsing: find seat checkboxes and infer availability
            org.jsoup.nodes.Document doc = parser.getDocument();
            org.jsoup.select.Elements inputs = doc.select("input[type=checkbox][id^=seat_number]");

            for (org.jsoup.nodes.Element inputEl : inputs) {
                String rawId = inputEl.attr("id");
                String title = inputEl.attr("title");

                // Parent div may carry style background for accessible (disabled-person) seats
                org.jsoup.nodes.Element parent = inputEl.parent();
                String parentStyle = parent != null ? parent.attr("style") : "";

                boolean styleIndicatesAccessible = parentStyle != null && (parentStyle.toLowerCase().contains("#ff0")
                        || parentStyle.toLowerCase().contains("background"));
                boolean disabled = inputEl.hasAttr("disabled") || "true".equalsIgnoreCase(inputEl.attr("aria-disabled"));

                String status;
                if (disabled) {
                    // explicitly disabled checkbox => unavailable
                    status = "unavailable";
                } else if (styleIndicatesAccessible) {
                    // seats styled with yellow/background indicate accessible (for disabled persons)
                    status = "accessible";
                } else {
                    // default: not disabled and not accessible-styled => available
                    status = "available";
                }

                Map<String, String> seat = new HashMap<>();
                seat.put("id", rawId != null ? rawId : "");
                seat.put("label", title != null ? title : "");
                seat.put("status", status);
                seats.add(seat);
            }

            // Parse metadata and pricing from embedded seat page script
            Map<String, Object> extracted = extractSeatPageData(seatPage.html());
            seatPageMetadata = (Map<String, String>) extracted.get("metadata");
            seatPagePricing = (Map<String, BigDecimal>) extracted.get("pricing");

            Map<String, Object> result = new HashMap<>();
            result.put("seats", seats);
            result.put("metadata", seatPageMetadata);
            result.put("pricing", seatPagePricing);

            if (!seats.isEmpty()) {
                logger.info("Parsed {} seats from seat page", seats.size());
                return result;
            } else {
                logger.warn("No seat checkboxes found on seat page, returning empty result");
                return result;
            }
        } catch (Exception e) {
            logger.error("Error fetching seats", e);
            throw new IOException("Failed to fetch seats", e);
        }
    }

    private Map<String, Object> extractSeatPageData(String html) {
        Map<String, String> metadata = new HashMap<>();
        Map<String, BigDecimal> pricing = new HashMap<>();

        // Pricing-related fields
        java.util.Set<String> pricingFields = new java.util.HashSet<>();
        pricingFields.add("child_price");
        pricingFields.add("adult_price");
        pricingFields.add("adult_insurance_price");
        pricingFields.add("child_insurance_price");
        pricingFields.add("os_child_price");
        pricingFields.add("os_adult_price");

        // Parse JS assignments first
        Pattern scriptPattern = Pattern.compile("document\\.getElementById\\(\\\"([^\\\"]+)\\\"\\)\\.(?:value|innerHTML)\\s*=\\s*\\\"([^\\\"]*)\\\";", Pattern.CASE_INSENSITIVE);
        Matcher matcher = scriptPattern.matcher(html);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            if (pricingFields.contains(key)) {
                try {
                    pricing.put(key, new BigDecimal(value));
                } catch (NumberFormatException ex) {
                    logger.warn("Unable to parse pricing value for {}: {}", key, value);
                }
            } else {
                metadata.put(key, value);
            }
        }

        // Parse HTML blocks like label/value pairs from metadata containers. The label div may be present but empty,
        // so use the strong value nodes and fall back by order when the label text is missing.
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        org.jsoup.select.Elements strongElements = doc.select("div > strong");

        java.util.List<String> fallbackValues = new ArrayList<>();
        java.util.List<String> metadataFallbackOrder = java.util.Arrays.asList(
            "route",
            "departure_datetime",
            "company_name",
            "bus_model",
            "plate_number"
        );

        for (org.jsoup.nodes.Element strong : strongElements) {
            String value = strong.text().trim();
            org.jsoup.nodes.Element container = strong.parent();
            org.jsoup.nodes.Element labelElement = container != null ? container.previousElementSibling() : null;

            String key = null;
            String labelText = labelElement.text().trim().replace("\u00A0", " ");
            if (labelText != null && !labelText.isEmpty()) {
                logger.debug("Extracting seat page metadata - found label: '{}', value: '{}'", labelElement.text(), value);

                if (labelText.endsWith(":")) {
                    labelText = labelText.substring(0, labelText.length() - 1).trim();
                }
                if (!labelText.isEmpty()) {
                    key = normalizeSeatPageLabel(labelText);
                }
            }

            if (key != null && !key.isEmpty()) {
                if (!metadata.containsKey(key)) {
                    metadata.put(key, value);
                }
            } else if (!value.isEmpty()) {
                fallbackValues.add(value);
            }
        }

        if (!fallbackValues.isEmpty()) {
            java.util.List<String> remainingKeys = new ArrayList<>();
            for (String expectedKey : metadataFallbackOrder) {
                if (!metadata.containsKey(expectedKey)) {
                    remainingKeys.add(expectedKey);
                }
            }
            for (int i = 0; i < fallbackValues.size() && i < remainingKeys.size(); i++) {
                String fallbackKey = remainingKeys.get(i);
                String fallbackValue = fallbackValues.get(i);
                metadata.put(fallbackKey, fallbackValue);
                logger.debug("Assigned fallback metadata {} = {}", fallbackKey, fallbackValue);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("metadata", metadata);
        result.put("pricing", pricing);
        return result;
    }

    private String normalizeSeatPageLabel(String label) {
        switch (label) {
            case "Чиглэл":
                return "route";
            case "Хөдлөх огноо":
                return "departure_datetime";
            case "ААН нэр":
                return "company_name";
            case "Марк загвар":
                return "bus_model";
            case "Улсын дугаар":
                return "plate_number";
            default:
                return label.replaceAll("\\s+", "_").toLowerCase();
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

    public TransDepSession getSession() {
        return session;
    }

    public void setSession(TransDepSession session) {
        this.session = session;
        if (session != null) {
            if (session.getCookieJar() != null && crawler != null) {
                crawler.setCookieJar(session.getCookieJar());
            }
            this.departure = session.getDeparture();
            this.stop = session.getStop();
            this.destination = session.getDestination();
            this.dispatcherId = session.getDispatcherId();
        }
    }

    public org.apache.http.client.CookieStore getCookieJar() {
        return crawler != null ? crawler.getCookieJar() : null;
    }

    /**
     * Get metadata exposed by the seat page (route, departure time, company, bus model, plate number, etc.)
     */
    public Map<String, String> getSeatPageMetadata() {
        return seatPageMetadata;
    }

    /**
     * Get pricing exposed by the seat page script (child/adult prices, insurance costs, etc.)
     */
    public Map<String, BigDecimal> getSeatPagePricing() {
        return seatPagePricing;
    }

    /**
     * Set dispatcher ID (usually extracted from date selection)
     */
    public void setDispatcherId(String dispatcherId) throws IOException {
        validateSession("setDispatcherId");
        this.dispatcherId = dispatcherId;
        if (session != null) {
            session.setDispatcherId(dispatcherId);
        }
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
