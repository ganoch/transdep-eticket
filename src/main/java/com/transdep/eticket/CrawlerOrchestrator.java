package com.transdep.eticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.transdep.eticket.core.WebCrawler;
import com.transdep.eticket.core.WebCrawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrator for executing crawling tasks with defined steps
 * Manages the lifecycle of crawlers and execution flow
 */
public class CrawlerOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerOrchestrator.class);

    private CrawlerConfig config;
    private List<CrawlStep> steps;
    private WebCrawler crawler;

    public CrawlerOrchestrator(CrawlerConfig config) {
        this.config = config;
        this.steps = new ArrayList<>();
        logger.info("Orchestrator initialized with config: {}", config);
    }

    /**
     * Add a step to be executed
     */
    public void addStep(CrawlStep step) {
        this.steps.add(step);
        logger.debug("Step added: {}", step.getName());
    }

    /**
     * Execute all configured steps
     */
    public void execute() throws IOException {
        try {
            // Initialize crawler
            initializeCrawler();

            // Execute all steps
            for (CrawlStep step : steps) {
                logger.info("Executing step: {}", step.getName());
                if (config.isVerbose()) {
                    logger.info("Step description: {}", step.getDescription());
                }

                try {
                    step.execute(crawler, config);
                    logger.info("Step completed: {}", step.getName());
                } catch (Exception e) {
                    logger.error("Step failed: {}", step.getName(), e);
                    if (step.isFailFast()) {
                        throw new IOException("Step execution failed: " + step.getName(), e);
                    }
                }

                // Add delay between steps
                if (config.getDelayMs() > 0) {
                    try {
                        Thread.sleep(config.getDelayMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Sleep interrupted");
                    }
                }
            }

            logger.info("All steps completed successfully");

        } finally {
            cleanup();
        }
    }

    /**
     * Initialize the crawler based on configuration
     */
    private void initializeCrawler() throws IOException {
        logger.info("Initializing crawler...");
        this.crawler = CrawlerFactory.createCrawler(config);
        logger.info("Crawler initialized: {}", crawler.getClass().getSimpleName());
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        if (crawler != null) {
            try {
                crawler.close();
                logger.info("Crawler closed successfully");
            } catch (IOException e) {
                logger.error("Error closing crawler", e);
            }
        }
    }

    /**
     * Get the current crawler instance
     */
    public WebCrawler getCrawler() {
        return crawler;
    }

    /**
     * Get the configuration
     */
    public CrawlerConfig getConfig() {
        return config;
    }
}
