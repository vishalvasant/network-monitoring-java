package com.networkmonitoring.collector;

import com.networkmonitoring.logging.CustomLogger;
import com.networkmonitoring.logging.LogLevel;

/**
 * Example data collector for basic system metrics (placeholder).
 * In a real application, this would interact with OS APIs or libraries
 * to get actual network traffic, CPU, memory, etc.
 */
public class SystemMetricsCollector implements DataCollector {
    private static final String COLLECTOR_NAME = "SystemMetricsCollector";

    @Override
    public void collectData(CustomLogger logger) {
        // Placeholder: Simulate collecting some data
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long usedMemory = totalMemory - freeMemory;

        logger.info(String.format("Memory Usage: Used=%d MB, Free=%d MB, Total=%d MB", 
                                 usedMemory / (1024 * 1024),
                                 freeMemory / (1024 * 1024),
                                 totalMemory / (1024 * 1024)), 
                    getCollectorName());

        // Simulate some network activity log
        logger.debug("Simulated network packet count: " + (int)(Math.random() * 1000), getCollectorName());

        // Simulate a warning
        if (Math.random() < 0.1) { // 10% chance of a warning
            logger.warn("Simulated high latency detected on eth0", getCollectorName());
        }
    }

    @Override
    public String getCollectorName() {
        return COLLECTOR_NAME;
    }
}
