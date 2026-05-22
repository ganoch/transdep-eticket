# Website Crawler Library Boilerplate

A Java library for crawling legacy 2010-ish websites that return HTML instead of JSON on AJAX requests.

## Project Structure

```
src/main/java/com/crawler/transdep/eticket/
├── core/
│   ├── WebCrawler.java              # Base crawler for simple requests
│   └── StatefulWebCrawler.java      # Crawler with session/cookie management
├── parser/
│   └── HtmlParser.java              # HTML parsing utilities (Jsoup-based)
├── example/
│   ├── ExampleWebsiteCrawler.java   # Basic crawler example
│   └── ExampleLoginCrawler.java     # Stateful crawler with login example
├── CrawlerRunner.java               # Main entry point
├── CrawlerOrchestrator.java         # Orchestrates crawler execution
├── CrawlerFactory.java              # Factory for creating crawlers
├── CrawlerConfig.java               # Configuration container
├── CrawlStep.java                   # Interface for defining steps
└── CommandLineParser.java           # CLI argument parsing
```

## Dependencies

- **httpclient** (4.5.14) - HTTP requests with cookie/session support
- **jsoup** (1.15.3) - HTML parsing and DOM traversal
- **slf4j** (1.7.36) - Logging
- Jackson - JSON parsing (already in project)

## Command Line Usage

### Basic Crawl
```bash
java -cp target/classes com.crawler.transdep.eticket.CrawlerRunner \
  --url=https://example-legacy-website.com \
  --type=basic
```

### Login Crawl
```bash
java -cp target/classes com.crawler.transdep.eticket.CrawlerRunner \
  --url=https://example-legacy-website.com \
  --type=login \
  --username=myuser \
  --password=mypass \
  --verbose
```

### With Output to File
```bash
java -cp target/classes com.crawler.transdep.eticket.CrawlerRunner \
  --url=https://example-legacy-website.com \
  --output-format=json \
  --output-path=/tmp/results.json
```

### Available Options
- `--url=<URL>` - Base URL of the website (required)
- `--type=basic|login|stateful` - Crawler type (default: basic)
- `--username=<USERNAME>` - Username for login crawlers
- `--password=<PASSWORD>` - Password for login crawlers
- `--delay=<MS>` - Delay between requests in milliseconds (default: 1000)
- `--timeout=<MS>` - Request timeout in milliseconds (default: 30000)
- `--output-format=console|json|csv` - Output format (default: console)
- `--output-path=<PATH>` - Output file path for non-console formats
- `--verbose, -v` - Enable verbose logging
- `--help, -h` - Show help message

## Quick Start

### 1. Simple Web Crawler (No Login Required)

Extend `WebCrawler` for basic crawling:

```java
public class MyWebsiteCrawler extends WebCrawler {
    public MyWebsiteCrawler(String baseUrl) {
        super(baseUrl);
    }

    @Override
    public void crawl() throws IOException {
        Document page = getPage("/data");
        HtmlParser parser = new HtmlParser(page);

        List<String> items = parser.getTextList(".item-class");
        items.forEach(System.out::println);
    }
}
```

### 2. Stateful Crawler (With Login)

Extend `StatefulWebCrawler` for websites requiring authentication:

```java
public class MyLoginCrawler extends StatefulWebCrawler {
    public MyLoginCrawler(String baseUrl, String username, String password) {
        super(baseUrl);
        // ... authentication logic
    }

    @Override
    public void crawl() throws IOException {
        // login() -> fetchProtectedData() flow
    }
}
```

## Core Classes

### WebCrawler

**Methods:**
- `getPage(String path)` - GET request, returns parsed Document
- `postPage(String path, String formData)` - POST with form data
- `postJson(String path, String jsonBody)` - POST with JSON body
- `addDefaultHeader(String name, String value)` - Set headers for all requests
- `close()` - Cleanup HTTP client

### StatefulWebCrawler

Extends `WebCrawler` with session management:
- `getPageStateful(String path)` - GET with cookie persistence
- `postPageStateful(String path, String formData)` - POST with cookie persistence
- `getCookies()` - List all cookies
- `getCookieValue(String name)` - Get specific cookie
- `logCookies()` - Debug: print all cookies

### HtmlParser

Utility class for parsing HTML Documents (Jsoup):

```java
Document doc = crawler.getPage("/page");
HtmlParser parser = new HtmlParser(doc);

// Text extraction
String title = parser.getText("h1");
List<String> items = parser.getTextList(".item");

// Attribute extraction
String href = parser.getAttributeValue("a.link", "href");

// Table parsing
List<Map<String, String>> data = parser.parseTable(
    "table#data",      // table selector
    "thead th",        // header row
    "tbody tr"         // data rows
);

// Form extraction
Map<String, String> formFields = parser.parseForm("form#search");

// Raw HTML
String html = parser.getHtml(".content");

// Element existence check
if (parser.hasElement(".error")) {
    // handle error
}
```

## Usage Examples

### Example 1: Parse a Table

```java
WebCrawler crawler = new WebCrawler("https://legacy-site.com") {
    @Override
    public void crawl() throws IOException {
        Document page = getPage("/products");
        HtmlParser parser = new HtmlParser(page);

        List<Map<String, String>> products = parser.parseTable(
            "table.products",
            "thead th",
            "tbody tr"
        );

        products.forEach(row -> {
            System.out.println(row.get("Product Name") + " - $" + row.get("Price"));
        });
    }
};
crawler.crawl();
crawler.close();
```

### Example 2: Submit a Form

```java
WebCrawler crawler = new WebCrawler("https://legacy-site.com") {
    @Override
    public void crawl() throws IOException {
        // Get the form first (to capture hidden fields, CSRF tokens, etc.)
        Document formPage = getPage("/search");
        HtmlParser formParser = new HtmlParser(formPage);
        Map<String, String> formData = formParser.parseForm("form#search");

        // Add user input
        formData.put("query", "laptop");

        // Submit form
        String formBody = formData.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        Document results = postPage("/search", formBody);
        HtmlParser resultParser = new HtmlParser(results);
Programmatic Usage

### Using the Orchestrator with Custom Steps

```java
// Create configuration
CrawlerConfig config = new CrawlerConfig();
config.setBaseUrl("https://example.com");
config.setCrawlerType("basic");
config.setVerbose(true);

// Create orchestrator
CrawlerOrchestrator orchestrator = new CrawlerOrchestrator(config);

// Define custom steps
orchestrator.addStep(new CrawlStep() {
    @Override
    public String getName() {
        return "step-1-fetch-data";
    }

    @Override
    public String getDescription() {
        return "Fetch and parse data table";
    }

    @Override
    public void execute(WebCrawler crawler, CrawlerConfig config) throws Exception {
        Document page = crawler.getPage("/data");
        HtmlParser parser = new HtmlParser(page);
        List<Map<String, String>> table = parser.parseTable(
            "table#data", "thead th", "tbody tr"
        );
        System.out.println("Found " + table.size() + " rows");
    }
});

// Execute
orchestrator.execute();
```

### Creating a Custom Crawler Class

```java
public class MyTransdepCrawler extends WebCrawler {
    public MyTransdepCrawler(String baseUrl) {
        super(baseUrl);
    }

    @Override
    public void crawl() throws IOException {
        // Your custom crawling logic
        Document page = getPage("/");
        HtmlParser parser = new HtmlParser(page);
        // ... parse and process
    }
}
```

## Building

```bash
mvn clean compile
mvn package
```

## Running Tests

```bash
mvn test
```

## Next Steps

1. **Customize CrawlerRunner** - Modify `addDefaultSteps()` in `CrawlerRunner` for your specific crawling logic
2. **Create Domain-Specific Crawlers** - Extend `WebCrawler` or `StatefulWebCrawler` for your website
3. **Define Crawl Steps** - Implement `CrawlStep` interface for modular crawling tasks
4. **Test with Your Website** - Update CSS selectors to match your target website's DOM structure
5. **Configure Output** - Use `--output-format` and `--output-path` for different output formats
6. **Deploy** - Package as JARname",
    "password"
);
crawler.crawl();
crawler.close();
```

## Tips for Legacy Websites

1. **Inspect DOM carefully** - Use browser DevTools to find the right CSS selectors
2. **Handle hidden fields** - Always fetch the form first to get CSRF tokens, session IDs, etc.
3. **Check AJAX headers** - Some sites send `X-Requested-With: XMLHttpRequest` headers
4. **Manage rate limiting** - Add delays between requests if needed:
   ```java
   Thread.sleep(1000); // 1 second delay
   ```
5. **Handle JavaScript-rendered content** - Jsoup can't execute JS. If content is rendered by JS, you may need Selenium or Playwright instead
6. **Set appropriate User-Agent** - Some sites block crawlers. Defaults are provided but can be customized

## Logging

Configure logging in your `log4j2.xml` or `logback.xml`:

```xml
<logger name="com.crawler" level="DEBUG"/>
```

## Building

```bash
mvn clean compile
mvn package
```

## Next Steps

1. Copy and modify `ExampleWebsiteCrawler` for your specific website
2. Update selectors to match the actual website DOM structure
3. Add custom parsing logic as needed
4. Deploy or integrate into your application
