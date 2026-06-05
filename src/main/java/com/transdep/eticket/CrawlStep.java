package com.transdep.eticket;

import com.transdep.eticket.core.WebCrawler;

/**
 * Interface for defining individual crawling steps/tasks
 * Each step represents a specific crawling operation
 */
public interface CrawlStep {

    /**
     * Get the name of this step
     */
    String getName();

    /**
     * Get the description of what this step does
     */
    String getDescription();

    /**
     * Execute the step
     */
    void execute(WebCrawler crawler, CrawlerConfig config) throws Exception;

    /**
     * Whether to fail fast if this step fails
     */
    default boolean isFailFast() {
        return true;
    }
}
