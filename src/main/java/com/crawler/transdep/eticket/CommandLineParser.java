package com.crawler.transdep.eticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse command line arguments and populate CrawlerConfig
 */
public class CommandLineParser {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineParser.class);

    /**
     * Parse command line arguments
     *
     * Usage:
     *   --url=<url>
     *   --type=basic|login|stateful
     *   --username=<username>
     *   --password=<password>
     *   --delay=<ms>
     *   --timeout=<ms>
     *   --output-format=console|json|csv
     *   --output-path=<path>
     *   --verbose
     *   --help
     */
    public static CrawlerConfig parse(String[] args) {
        CrawlerConfig config = new CrawlerConfig();

        if (args == null || args.length == 0) {
            printHelp();
            return config;
        }

        for (String arg : args) {
            if (arg.startsWith("--")) {
                parseArgument(arg.substring(2), config);
            } else if (arg.startsWith("-")) {
                parseShortArgument(arg.substring(1), config);
            }
        }

        return config;
    }

    private static void parseArgument(String arg, CrawlerConfig config) {
        if (arg.equals("help")) {
            printHelp();
            System.exit(0);
        } else if (arg.equals("verbose")) {
            config.setVerbose(true);
        } else if (arg.contains("=")) {
            String[] parts = arg.split("=", 2);
            String key = parts[0];
            String value = parts[1];

            switch (key) {
                case "url":
                    config.setBaseUrl(value);
                    break;
                case "type":
                    config.setCrawlerType(value);
                    break;
                case "username":
                    config.setUsername(value);
                    break;
                case "password":
                    config.setPassword(value);
                    break;
                case "delay":
                    config.setDelayMs(Integer.parseInt(value));
                    break;
                case "timeout":
                    config.setTimeoutMs(Integer.parseInt(value));
                    break;
                case "output-format":
                    config.setOutputFormat(value);
                    break;
                case "output-path":
                    config.setOutputPath(value);
                    break;
                default:
                    logger.warn("Unknown argument: {}", key);
            }
        }
    }

    private static void parseShortArgument(String arg, CrawlerConfig config) {
        switch (arg) {
            case "h":
            case "help":
                printHelp();
                System.exit(0);
                break;
            case "v":
            case "verbose":
                config.setVerbose(true);
                break;
            default:
                logger.warn("Unknown short argument: {}", arg);
        }
    }

    private static void printHelp() {
        System.out.println("""
                Website Crawler - Command Line Usage

                Usage: java -cp target/classes com.crawler.transdep.eticket.CrawlerRunner [options]

                Options:
                  --url=<URL>                  Base URL of the website to crawl (required)
                  --type=<TYPE>                Crawler type: basic, login, stateful (default: basic)
                  --username=<USERNAME>        Username for login crawlers
                  --password=<PASSWORD>        Password for login crawlers
                  --delay=<MS>                 Delay between requests in milliseconds (default: 1000)
                  --timeout=<MS>               Request timeout in milliseconds (default: 30000)
                  --output-format=<FORMAT>     Output format: console, json, csv (default: console)
                  --output-path=<PATH>         Output file path for non-console formats
                  --verbose, -v                Enable verbose logging
                  --help, -h                   Show this help message

                Examples:
                  Basic crawl:
                    java -cp target/classes com.crawler.transdep.eticket.CrawlerRunner \\
                      --url=https://example.com --type=basic

                  Login crawl:
                    java -cp target/classes com.crawler.transdep.eticket.CrawlerRunner \\
                      --url=https://example.com --type=login \\
                      --username=myuser --password=mypass \\
                      --verbose

                  With output file:
                    java -cp target/classes com.crawler.transdep.eticket.CrawlerRunner \\
                      --url=https://example.com --output-format=json \\
                      --output-path=/tmp/results.json
                """);
    }

    /**
     * Validate configuration
     */
    public static void validate(CrawlerConfig config) {
        if (config.getBaseUrl() == null || config.getBaseUrl().isEmpty()) {
            throw new IllegalArgumentException("--url parameter is required");
        }

        if ("login".equals(config.getCrawlerType())) {
            if (config.getUsername() == null || config.getUsername().isEmpty()) {
                throw new IllegalArgumentException("--username is required for login crawler");
            }
            if (config.getPassword() == null) {
                throw new IllegalArgumentException("--password is required for login crawler");
            }
        }
    }
}
