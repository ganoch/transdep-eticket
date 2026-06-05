package com.transdep.eticket.example;

import com.transdep.eticket.parser.HtmlParser;
import com.transdep.eticket.core.WebCrawler;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.transdep.eticket.core.WebCrawler;
import com.transdep.eticket.parser.HtmlParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Example crawler implementation for a legacy website
 * Adapt this template to your specific website structure
 */
public class ExampleWebsiteCrawler extends WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(ExampleWebsiteCrawler.class);

    public ExampleWebsiteCrawler(String baseUrl) {
        super(baseUrl);
    }

    @Override
    public void crawl() throws IOException {
        logger.info("Starting crawl for {}", baseUrl);

        // Example: Fetch homepage
        fetchHomepage();

        // Example: Fetch and parse a table
        fetchTableData();

        // Example: Submit form and parse response
        submitFormAndParse();
    }

    /**
     * Example: Fetch and parse homepage
     */
    private void fetchHomepage() throws IOException {
        logger.info("Fetching homepage...");
        Document page = getPage("/");
        HtmlParser parser = new HtmlParser(page);

        // Extract title
        String title = parser.getText("h1");
        logger.info("Page title: {}", title);
    }

    /**
     * Example: Fetch and parse table data
     */
    private void fetchTableData() throws IOException {
        logger.info("Fetching table data...");
        Document page = getPage("/data");
        HtmlParser parser = new HtmlParser(page);

        // Parse table: adjust selectors to match your website
        List<Map<String, String>> tableData = parser.parseTable(
            "table#data-table",      // table selector
            "thead th",              // header selector
            "tbody tr"               // row selector
        );

        logger.info("Extracted {} rows from table", tableData.size());
        tableData.forEach(row -> logger.debug("Row: {}", row));
    }

    /**
     * Example: Submit form and parse response
     */
    private void submitFormAndParse() throws IOException {
        logger.info("Submitting form...");

        // First get the form to extract hidden fields
        Document formPage = getPage("/search-form");
        HtmlParser formParser = new HtmlParser(formPage);

        Map<String, String> formData = formParser.parseForm("form#search");
        logger.info("Extracted form fields: {}", formData.keySet());

        // Add your search parameters
        formData.put("q", "example search");
        formData.put("page", "1");

        // Submit form
        StringBuilder formBody = new StringBuilder();
        formData.forEach((key, value) -> {
            if (formBody.length() > 0) formBody.append("&");
            formBody.append(key).append("=").append(value);
        });

        Document resultPage = postPage("/search", formBody.toString());
        HtmlParser resultParser = new HtmlParser(resultPage);

        // Parse results
        List<String> results = resultParser.getTextList(".result-item");
        logger.info("Found {} results", results.size());
        results.forEach(result -> logger.debug("Result: {}", result));
    }

    /**
     * Example: Fetch data via POST with JSON body
     * (even though response is HTML)
     */
    private void fetchWithJsonPost() throws IOException {
        String jsonBody = "{\"filter\": \"active\", \"limit\": 10}";
        Document page = postJson("/api/items", jsonBody);
        HtmlParser parser = new HtmlParser(page);

        List<String> items = parser.getTextList(".item");
        logger.info("Fetched {} items", items.size());
    }

    public static void main(String[] args) {
        ExampleWebsiteCrawler crawler = null;
        try {
            crawler = new ExampleWebsiteCrawler("https://example-legacy-website.com");
            crawler.crawl();
        } catch (IOException e) {
            logger.error("Error during crawling", e);
        } finally {
            if (crawler != null) {
                try {
                    crawler.close();
                } catch (IOException e) {
                    logger.error("Error closing crawler", e);
                }
            }
        }
    }
}
