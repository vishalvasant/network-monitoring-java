package com.networkmonitoring.collector;

import com.networkmonitoring.logging.CustomLogger;

/**
 * Interface for different types of data collectors.
 */
public interface DataCollector {
    void collectData(CustomLogger logger);
    String getCollectorName();
}
