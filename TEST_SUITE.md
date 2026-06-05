# TransDep E-Ticket Crawler - Test Suite

Complete test suite for testing the TransDep E-Ticket crawler library.

## Running Tests

### Option 1: Run All Tests with Maven

```bash
cd /home/ochoo/projects/transdep-eticket
mvn clean test
```

### Option 2: Run Specific Test Class

```bash
# Run unit tests only
mvn test -Dtest=TransDepEticketTest

# Run integration tests only
mvn test -Dtest=TransDepEticketIntegrationTest
```

### Option 3: Run Test Runner Directly (after building)

```bash
# Build the project
mvn clean compile test-compile

# Run the test runner
java -cp target/classes:target/test-classes com.transdep.eticket.TestRunner
```

### Option 4: Run with Maven Surefire Plugin

```bash
mvn surefire:test
```

## Test Coverage

### Unit Tests (TransDepEticketTest)

1. **testInitialization** - Verifies proper initialization of TransDepEticket
2. **testFetchDepartures** - Tests fetching available departure stations
3. **testFetchDestinations** - Tests fetching available destination stations
4. **testDepartureAndDestinationCaching** - Verifies page caching works (both calls should reuse same page)
5. **testFetchDatesWithoutDeparture** - Verifies proper error handling when state is incomplete
6. **testSetDeparture** - Tests setting departure station
7. **testSetDestination** - Tests setting destination station
8. **testGettersAndSetters** - Tests all getter/setter methods
9. **testCacheClear** - Tests cache clearing functionality

### Integration Tests (TransDepEticketIntegrationTest)

1. **testFullWorkflow** - Complete workflow: Departures → Destinations → Set both → Fetch dates
2. **testTripsWorkflow** - Extended workflow including trip fetching
3. **testMultipleDepartureDestinationChanges** - Tests changing selections multiple times

## Test Output Example

```
╔══════════════════════════════════════════════════════════════╗
║     TransDep E-Ticket Crawler Test Suite                     ║
╚══════════════════════════════════════════════════════════════╝

Running unit tests...
──────────────────────────────────────────────────────────────
10:30:45.123 [main] INFO  com.transdep.eticket.TransDepEticketTest - Test: Initialization
10:30:45.456 [main] INFO  com.transdep.eticket.TransDepEticketTest - ✓ Initialization test passed
...

Running integration tests...
──────────────────────────────────────────────────────────────
10:31:15.789 [main] INFO  com.transdep.eticket.TransDepEticketIntegrationTest - Test: Full Workflow
10:31:16.234 [main] INFO  com.transdep.eticket.TransDepEticketIntegrationTest - Step 1: Fetching departures...
...

╔══════════════════════════════════════════════════════════════╗
║                      TEST SUMMARY                            ║
╠══════════════════════════════════════════════════════════════╣
║ Total Tests Run:  9                                           ║
║ Failures:         0                                           ║
║ Ignored:          0                                           ║
╠══════════════════════════════════════════════════════════════╣
║ Status: ✓ ALL TESTS PASSED                                   ║
╚══════════════════════════════════════════════════════════════╝
```

## Requirements

- JDK 1.8+
- Maven 3.6+
- Internet connection (tests require access to https://eticket.transdep.mn/)

## Dependencies

The test suite uses:
- **JUnit 4.13.2** - Test framework
- **Mockito 4.11.0** - Mocking framework (available if needed)
- **Logback 1.2.11** - Logging implementation for tests

## Test Configuration

Test logging is configured in [src/test/resources/logback.xml](src/test/resources/logback.xml):
- Console appender with timestamp and log level
- DEBUG level for crawler package
- INFO level for TransDep eticket package

## Troubleshooting

### "Connection refused" or timeout errors
- Verify you have internet access
- Check if https://eticket.transdep.mn/ is accessible
- Increase timeout in test if website is slow

### "No such file or directory" errors
- Ensure you're running from the project root: `/home/ochoo/projects/transdep-eticket`
- Run `mvn clean` first to remove old build artifacts

### Logging not showing
- Tests use logback configuration from `src/test/resources/logback.xml`
- Check that the file exists
- Verify logback-classic is in the classpath

## Continuous Integration

To use in CI/CD pipeline:

```bash
# Build and run tests
mvn clean test

# Check exit code
echo $?
```

Exit code 0 = all tests passed
Exit code 1 = tests failed

## Adding New Tests

1. Create test class in `src/test/java/com/crawler/transdep/eticket/`
2. Extend with `@Test` annotations using JUnit 4
3. Use `@Before` and `@After` for setup/teardown
4. Run with Maven or TestRunner

Example:

```java
@Test
public void testMyFeature() throws IOException {
    TransDepEticket eticket = new TransDepEticket();
    // Your test code
    eticket.close();
}
```

## Performance Notes

- Tests make actual network requests to the website
- Each test takes 1-2 seconds minimum
- Full suite runs in ~30-60 seconds depending on network
- Page caching is tested to ensure only 1 request is made for departures + destinations

## License

Same as the main project
