package com.transdep.eticket.core;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.transdep.eticket.util.HttpClientBuilder;
import com.transdep.eticket.util.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Stateful crawler for websites that require session management
 * (e.g., login, multi-step forms, CSRF tokens)
 */
public abstract class StatefulWebCrawler extends WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(StatefulWebCrawler.class);

    protected HttpClientContext context;
    protected CookieStore cookieStore;

    public StatefulWebCrawler(String baseUrl) {
        super(baseUrl);
        this.cookieStore = new BasicCookieStore();
        this.context = HttpClientContext.create();
        this.context.setCookieStore(cookieStore);
        try {
            this.httpClient = HttpClientBuilder.createDefaultHttpClient();
        } catch (Exception e) {
            logger.error("Failed to create HTTP client", e);
            throw new RuntimeException(e);
        }
    }


    public Document getPageStateful(String path) throws IOException {
        return getPageStateful(path, false);
    }

    /**
     * Perform stateful GET request
     */
    public Document getPageStateful(String path, boolean isAjax) throws IOException {
        String url = baseUrl + path;
        HttpGet get = new HttpGet(url);

        defaultHeaders.forEach(get::setHeader);
        if (isAjax) {
            get.setHeader("X-Requested-With", "XMLHttpRequest");
            get.setHeader("Sec-Fetch-Dest", "empty");
            get.setHeader("Sec-Fetch-Mode", "cors");
            get.setHeader("Sec-Fetch-Site", "same-origin");
        }

        try (CloseableHttpResponse response = httpClient.execute(get, context)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " for " + url);
            }
            String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            // logger.debug("Response for {}: \n{}", url, html);
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
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.setEntity(new StringEntity(formData, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = httpClient.execute(post, context)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " for " + url);
            }
            String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
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
     * Get the internal cookie jar
     */
    public CookieStore getCookieJar() {
        return cookieStore;
    }

    /**
     * Replace the current cookie jar and keep the HttpClientContext in sync.
     */
    public void setCookieJar(CookieStore cookieJar) {
        if (cookieJar == null) {
            cookieJar = new BasicCookieStore();
        }
        this.cookieStore = cookieJar;
        if (this.context != null) {
            this.context.setCookieStore(cookieJar);
        }
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
