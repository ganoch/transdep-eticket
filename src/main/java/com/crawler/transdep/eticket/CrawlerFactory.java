package com.crawler.transdep.eticket;

import com.crawler.transdep.eticket.core.WebCrawler;
import com.crawler.transdep.eticket.core.StatefulWebCrawler;
import com.crawler.transdep.eticket.example.ExampleWebsiteCrawler;
import com.crawler.transdep.eticket.example.ExampleLoginCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating crawler instances based on configuration
 */
public class CrawlerFactory {
  private static final Logger logger = LoggerFactory.getLogger(CrawlerFactory.class);

  /**
   * Create a crawler instance based on the configuration type
   */
  public static WebCrawler createCrawler(CrawlerConfig config) {
    if (config.getBaseUrl() == null) {
      throw new IllegalArgumentException("baseUrl is required");
    }

    String crawlerType = config.getCrawlerType().toLowerCase();

    logger.info("Creating crawler of type: {}", crawlerType);

    switch (crawlerType) {
      case "basic":
        return createBasicCrawler(config);
      case "login":
        return createLoginCrawler(config);
      case "stateful":
        return createStatefulCrawler(config);
      default:
        throw new IllegalArgumentException("Unknown crawler type: " + crawlerType);
    }
    ;
  }

  /**
   * Create a basic crawler (no authentication)
   */
  private static WebCrawler createBasicCrawler(CrawlerConfig config) {
    return new ExampleWebsiteCrawler(config.getBaseUrl());
  }

  /**
   * Create a login-based crawler (requires username/password)
   */
  private static WebCrawler createLoginCrawler(CrawlerConfig config) {
    if (config.getUsername() == null || config.getPassword() == null) {
      throw new IllegalArgumentException("username and password are required for login crawler");
    }
    return new ExampleLoginCrawler(config.getBaseUrl(), config.getUsername(), config.getPassword());
  }

  /**
   * Create a stateful crawler (cookie/session management without login)
   */
  private static WebCrawler createStatefulCrawler(CrawlerConfig config) {
    return new ExampleLoginCrawler(config.getBaseUrl(), "anonymous", "");
  }
}
