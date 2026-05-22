package com.crawler.transdep.eticket.core;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateful crawler for websites that require session management
 * (e.g., login, multi-step forms, CSRF tokens)
 */
public abstract class StatefulWebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(StatefulWebCrawler.class);

    protected CloseableHttpClient httpClient;
    protected HttpClientContext context;
    protected CookieStore cookieStore;
    protected String baseUrl;
    protected Map<String, String> defaultHeaders;

    public StatefulWebCrawler(String baseUrl) {
        this.baseUrl = baseUrl;
        this.cookieStore = new BasicCookieStore();
        this.context = HttpClientContext.create();
        this.context.setCookieStore(cookieStore);
        this.httpClient = HttpClients.createDefault();
        this.defaultHeaders = new HashMap<>();
        this.defaultHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    }

    /**
     * Perform stateful GET request
     */
    public Document getPageStateful(String path) throws IOException {
        String url = baseUrl + path;
        HttpGet get = new HttpGet(url);
        defaultHeaders.forEach(get::setHeader);

        try (CloseableHttpResponse response = httpClient.execute(get, context)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " for " + url);
            }
            String html = EntityUtils.toString(response.getEntity());
            return Jsoup.parse(html, url);
        }
    }

    /**
     * Perform stateful POST request
     */
    public Document postPageStateful(String path, String formData) throws IOException {
        String url = baseUrl + path;
        HttpPost post = new HttpPost(url);
        defaultHeaders.forEach(post::setHeader);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity(formData));

        try (CloseableHttpResponse response = httpClient.execute(post, context)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " for " + url);
            }
            String html = EntityUtils.toString(response.getEntity());
            return Jsoup.parse(html, url);
        }
    }

    /**
     * Get all stored cookies
     */
    public List<Cookie> getCookies() {
        return cookieStore.getCookies();
    }

    /**
     * Get cookie value by name
     */
    public String getCookieValue(String name) {
        return cookieStore.getCookies().stream()
            .filter(cookie -> cookie.getName().equals(name))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    /**
     * Log all current cookies
     */
    public void logCookies() {
        logger.debug("Current cookies:");
        cookieStore.getCookies().forEach(cookie ->
            logger.debug("  {} = {}", cookie.getName(), cookie.getValue())
        );
    }

    /**
     * Add a default header
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
     * Abstract method for crawling logic
     */
    public abstract void crawl() throws IOException;
}
