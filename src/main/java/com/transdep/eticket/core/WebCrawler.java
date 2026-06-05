package com.transdep.eticket.core;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.transdep.eticket.util.HttpClientBuilder;
import com.transdep.eticket.util.HttpClientBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base crawler for handling legacy websites that return HTML instead of JSON.
 * Provides methods for GET/POST requests and HTML parsing.
 */
public abstract class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    protected CloseableHttpClient httpClient;
    protected String baseUrl;
    protected Map<String, String> defaultHeaders;

    public WebCrawler(String baseUrl) {
        this.baseUrl = baseUrl;
        try {
            this.httpClient = HttpClientBuilder.createDefaultHttpClient();
        } catch (Exception e) {
            logger.error("Failed to create HTTP client", e);
            throw new RuntimeException(e);
        }
        this.defaultHeaders = new HashMap<>();
        this.defaultHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        this.defaultHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }

    /**
     * Perform GET request and return HTML document
     */
    protected Document getPage(String path) throws IOException {
        String url = baseUrl + path;
        HttpGet get = new HttpGet(url);

        defaultHeaders.forEach(get::setHeader);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " for " + url);
            }
            String html = EntityUtils.toString(response.getEntity());
            return Jsoup.parse(html, url);
        }
    }

    /**
     * Perform POST request and return HTML document
     */
    protected Document postPage(String path, String formData) throws IOException {
        String url = baseUrl + path;
        HttpPost post = new HttpPost(url);

        defaultHeaders.forEach(post::setHeader);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity(formData));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " for " + url);
            }
            String html = EntityUtils.toString(response.getEntity());
            return Jsoup.parse(html, url);
        }
    }

    /**
     * Perform POST request with JSON body
     */
    protected Document postJson(String path, String jsonBody) throws IOException {
        String url = baseUrl + path;
        HttpPost post = new HttpPost(url);

        defaultHeaders.forEach(post::setHeader);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(jsonBody));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " for " + url);
            }
            String html = EntityUtils.toString(response.getEntity());
            return Jsoup.parse(html, url);
        }
    }

    /**
     * Add a default header to all requests
     */
    public void addDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
    }

    /**
     * Close the HTTP client
     */
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
            logger.info("HTTP client closed");
        }
    }

    /**
     * Abstract method to be implemented by subclasses
     * Define your crawling logic here
     */
    public abstract void crawl() throws IOException;
}
