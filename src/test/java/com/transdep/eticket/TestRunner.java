package com.transdep.eticket;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test runner for TransDepEticket crawler tests
 * Run with: java -cp target/classes:target/test-classes com.transdep.eticket.TestRunner
 */
public class TestRunner {
    private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     TransDep E-Ticket Crawler Test Suite                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        JUnitCore junit = new JUnitCore();

        // Run tests
        System.out.println("Running unit tests...");
        System.out.println(repeat("─", 62));
        Result unitResults = junit.run(TransDepEticketTest.class);

        System.out.println();
        System.out.println("Running integration tests...");
        System.out.println(repeat("─", 62));
        Result integrationResults = junit.run(TransDepEticketIntegrationTest.class);

        // Print summary
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      TEST SUMMARY                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        int totalTests = unitResults.getRunCount() + integrationResults.getRunCount();
        int totalFailures = unitResults.getFailureCount() + integrationResults.getFailureCount();
        int totalIgnored = unitResults.getIgnoreCount() + integrationResults.getIgnoreCount();
        boolean wasSuccessful = unitResults.wasSuccessful() && integrationResults.wasSuccessful();

        System.out.printf("║ Total Tests Run:  %-45d ║%n", totalTests);
        System.out.printf("║ Failures:         %-45d ║%n", totalFailures);
        System.out.printf("║ Ignored:          %-45d ║%n", totalIgnored);
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        if (wasSuccessful) {
            System.out.println("║ Status: ✓ ALL TESTS PASSED                                      ║");
        } else {
            System.out.println("║ Status: ✗ SOME TESTS FAILED                                    ║");
        }

        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // Print failures if any
        if (!wasSuccessful) {
            System.out.println();
            System.out.println("FAILURES:");
            System.out.println(repeat("─", 62));

            for (Failure failure : unitResults.getFailures()) {
                System.out.println("Unit Test: " + failure.getTestHeader());
                System.out.println(failure.getTrace());
                System.out.println();
            }

            for (Failure failure : integrationResults.getFailures()) {
                System.out.println("Integration Test: " + failure.getTestHeader());
                System.out.println(failure.getTrace());
                System.out.println();
            }
        }

        System.exit(wasSuccessful ? 0 : 1);
    }
}
