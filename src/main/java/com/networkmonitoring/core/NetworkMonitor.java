package com.networkmonitoring.core;

import com.networkmonitoring.collector.DataCollector;
import com.networkmonitoring.config.AppConfig;
import com.networkmonitoring.logging.CustomLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the network monitoring tasks.
 * Schedules data collection and manages the logger.
 */
public class NetworkMonitor {
    private final CustomLogger logger;
    private final List<DataCollector> dataCollectors;
    private final ScheduledExecutorService scheduler;
    private final AppConfig appConfig; // Future use for collector configs

    public NetworkMonitor(AppConfig appConfig, CustomLogger logger) {
        this.appConfig = appConfig;
        this.logger = logger;
        this.dataCollectors = new ArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(); // Simple scheduler for now
    }

    public void addDataCollector(DataCollector collector) {
        this.dataCollectors.add(collector);
        logger.info("Registered data collector: " + collector.getCollectorName(), "NetworkMonitor");
    }

    public void startMonitoring(long initialDelaySeconds, long periodSeconds) {
        if (dataCollectors.isEmpty()) {
            logger.warn("No data collectors registered. Monitoring will not start.", "NetworkMonitor");
            return;
        }

        scheduler.scheduleAtFixedRate(() -> {
            logger.debug("Starting data collection cycle.", "NetworkMonitor");
            for (DataCollector collector : dataCollectors) {
                try {
                    collector.collectData(logger);
                } catch (Exception e) {
                    logger.error("Error during data collection from " + collector.getCollectorName() + ": " + e.getMessage(), "NetworkMonitor");
                }
            }
            logger.debug("Data collection cycle finished.", "NetworkMonitor");
        }, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);

        logger.info("Network monitoring started. Collection interval: " + periodSeconds + " seconds.", "NetworkMonitor");
    }

    public void stopMonitoring() {
        logger.info("Attempting to stop network monitoring...", "NetworkMonitor");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.warn("Scheduler did not terminate gracefully, forcing shutdown.", "NetworkMonitor");
            } else {
                logger.info("Scheduler terminated gracefully.", "NetworkMonitor");
            }
        } catch (InterruptedException ie) {
            scheduler.shutdownNow();
            logger.error("Monitoring stop interrupted: " + ie.getMessage(), "NetworkMonitor");
            Thread.currentThread().interrupt();
        }
        logger.info("Network monitoring stopped.", "NetworkMonitor");
        if (logger != null) {
            logger.close();
        }
    }
}
