package com.crawler.transdep.eticket;

/**
 * Configuration class for crawler setup
 * Holds all configuration parameters passed via command line or configuration
 */
public class CrawlerConfig {
    private String baseUrl;
    private String username;
    private String password;
    private String crawlerType;
    private int delayMs;
    private int timeoutMs;
    private boolean verbose;
    private String outputFormat;  // json, csv, console
    private String outputPath;

    public CrawlerConfig() {
        this.delayMs = 1000;      // Default 1 second delay between requests
        this.timeoutMs = 30000;   // Default 30 second timeout
        this.verbose = false;
        this.outputFormat = "console";
        this.crawlerType = "basic";
    }

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCrawlerType() {
        return crawlerType;
    }

    public void setCrawlerType(String crawlerType) {
        this.crawlerType = crawlerType;
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public String toString() {
        return "CrawlerConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", crawlerType='" + crawlerType + '\'' +
                ", delayMs=" + delayMs +
                ", timeoutMs=" + timeoutMs +
                ", verbose=" + verbose +
                ", outputFormat='" + outputFormat + '\'' +
                ", outputPath='" + outputPath + '\'' +
                '}';
    }
}
