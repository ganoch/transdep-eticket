package com.crawler.transdep.eticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the crawler application
 * Parses command line arguments, sets up configuration, and executes crawling tasks
 */
public class CrawlerRunner {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerRunner.class);

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            CrawlerConfig config = CommandLineParser.parse(args);

            // Validate configuration
            CommandLineParser.validate(config);

            logger.info("Starting crawler with configuration: {}", config);

            // Create orchestrator
            CrawlerOrchestrator orchestrator = new CrawlerOrchestrator(config);

            // Add default steps (can be customized)
            addDefaultSteps(orchestrator);

            // Execute
            orchestrator.execute();

            logger.info("Crawling completed successfully");
            System.exit(0);

        } catch (IllegalArgumentException e) {
            logger.error("Configuration error: {}", e.getMessage());
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Crawling failed", e);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Add default crawling steps
     * Can be overridden or extended for custom crawling logic
     */
    private static void addDefaultSteps(CrawlerOrchestrator orchestrator) {
        // Step 1: Fetch homepage
        orchestrator.addStep(new CrawlStep() {
            @Override
            public String getName() {
                return "fetch-homepage";
            }

            @Override
            public String getDescription() {
                return "Fetch and analyze the homepage";
            }

            @Override
            public void execute(com.crawler.transdep.eticket.core.WebCrawler crawler, CrawlerConfig config) throws Exception {
                // This is a placeholder - customize with your logic
                logger.info("Fetching homepage from: {}", config.getBaseUrl());
                // Document page = crawler.getPage("/");
                // ... parse and process page
            }
        });

        // Step 2: Custom processing
        orchestrator.addStep(new CrawlStep() {
            @Override
            public String getName() {
                return "process-data";
            }

            @Override
            public String getDescription() {
                return "Process and extract data from website";
            }

            @Override
            public void execute(com.crawler.transdep.eticket.core.WebCrawler crawler, CrawlerConfig config) throws Exception {
                // This is a placeholder - customize with your logic
                logger.info("Processing data...");
                // Implement your data extraction logic here
            }
        });
    }

    /**
     * Create a custom orchestrator with your own steps
     */
    public static CrawlerOrchestrator createCustomOrchestrator(CrawlerConfig config) {
        return new CrawlerOrchestrator(config);
    }
}
