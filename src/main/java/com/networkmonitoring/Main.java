package com.networkmonitoring;

import com.networkmonitoring.collector.SystemMetricsCollector;
import com.networkmonitoring.config.AppConfig;
import com.networkmonitoring.core.NetworkMonitor;
import com.networkmonitoring.logging.CustomLogger;

/**
 * Main application entry point for the Network Monitoring System.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Network Monitoring System...");

        // Load configurations
        AppConfig appConfig = AppConfig.loadDefault();

        // Initialize logger
        CustomLogger logger = new CustomLogger(appConfig.getLogRotationConfig());
        logger.info("Application starting", "Main");

        // Initialize Network Monitor
        NetworkMonitor networkMonitor = new NetworkMonitor(appConfig, logger);

        // Register data collectors
        networkMonitor.addDataCollector(new SystemMetricsCollector());
        // Add other collectors here (e.g., for specific network interface traffic, ping tests, etc.)

        // Start monitoring (e.g., collect data every 60 seconds after an initial delay of 5s)
        long initialDelay = 5; // seconds
        long collectionPeriod = 60; // seconds
        networkMonitor.startMonitoring(initialDelay, collectionPeriod);

        // Keep the main thread alive or implement a proper shutdown mechanism
        // For this example, we'll let it run for a while then shut down.
        // In a real server application, this would run indefinitely or until a signal.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered. Stopping network monitor...", "Main");
            networkMonitor.stopMonitoring();
            logger.info("Application shut down gracefully.", "Main");
        }));

        // Simulate application running for some time
        try {
            // Keep alive for a long time for demo purposes if not running as a server
            // For a real daemon, this part would be different.
            // Thread.sleep(TimeUnit.MINUTES.toMillis(10)); // Example: run for 10 minutes
            System.out.println("Network Monitoring System is running. Press Ctrl+C to stop.");
            while(true) { // Keep main thread alive
                Thread.sleep(10000); // Sleep for a bit to avoid busy-waiting
            }
        } catch (InterruptedException e) {
            logger.warn("Main thread interrupted. Shutting down...", "Main");
            Thread.currentThread().interrupt(); // Preserve interrupt status
            // Shutdown hook will handle stopping the monitor
        } finally {
             // Ensure monitor is stopped if loop exits unexpectedly (though shutdown hook is preferred)
            if (!networkMonitor.scheduler.isShutdown()) {
                 networkMonitor.stopMonitoring();
            }
        }
    }
}
