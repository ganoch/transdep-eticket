package com.crawler.transdep.eticket.example;

import com.crawler.transdep.eticket.core.StatefulWebCrawler;
import com.crawler.transdep.eticket.parser.HtmlParser;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Example stateful crawler for websites requiring login/session management
 * Demonstrates how to maintain state across multiple requests
 */
public class ExampleLoginCrawler extends StatefulWebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(ExampleLoginCrawler.class);
    
    private String username;
    private String password;

    public ExampleLoginCrawler(String baseUrl, String username, String password) {
        super(baseUrl);
        this.username = username;
        this.password = password;
    }

    @Override
    public void crawl() throws IOException {
        logger.info("Starting stateful crawl for {}", baseUrl);
        
        // Step 1: Get login page and extract CSRF token
        if (!login()) {
            logger.error("Login failed");
            return;
        }
        
        // Step 2: Access protected content
        fetchProtectedData();
        
        // Log cookies to verify session
        logCookies();
    }

    /**
     * Login to the website
     */
    private boolean login() throws IOException {
        logger.info("Getting login page...");
        
        // Fetch login form
        Document loginPage = getPageStateful("/login");
        HtmlParser parser = new HtmlParser(loginPage);
        
        // Extract form fields (including CSRF token if present)
        Map<String, String> formData = parser.parseForm("form");
        logger.info("Login form fields: {}", formData.keySet());
        
        // Add credentials
        formData.put("username", username);
        formData.put("password", password);
        
        // Build form body
        StringBuilder formBody = new StringBuilder();
        formData.forEach((key, value) -> {
            if (formBody.length() > 0) formBody.append("&");
            formBody.append(key).append("=").append(value);
        });
        
        // Submit login form
        logger.info("Submitting login...");
        Document response = postPageStateful("/login", formBody.toString());
        
        // Check if login was successful (e.g., check for error message)
        return !response.selectFirst(".error-message") != null;
    }

    /**
     * Fetch protected data after login
     */
    private void fetchProtectedData() throws IOException {
        logger.info("Fetching protected data...");
        
        Document page = getPageStateful("/dashboard");
        HtmlParser parser = new HtmlParser(page);
        
        String username = parser.getText(".user-name");
        logger.info("Logged in as: {}", username);
        
        // Your protected data parsing logic here
    }

    public static void main(String[] args) {
        ExampleLoginCrawler crawler = null;
        try {
            crawler = new ExampleLoginCrawler(
                "https://example-legacy-website.com",
                "your-username",
                "your-password"
            );
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
