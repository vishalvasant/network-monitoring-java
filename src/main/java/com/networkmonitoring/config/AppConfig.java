package com.networkmonitoring.config;

/**
 * General Application Configuration placeholder.
 * Can be expanded to include configurations for data collectors, etc.
 */
public class AppConfig {
    private final LogRotationConfig logRotationConfig;
    // Add other configurations here, e.g., monitoring intervals, target IPs

    public AppConfig(LogRotationConfig logRotationConfig) {
        this.logRotationConfig = logRotationConfig;
    }

    public LogRotationConfig getLogRotationConfig() {
        return logRotationConfig;
    }

    public static AppConfig loadDefault() {
        // In a real app, this might load from a properties file or environment variables
        return new AppConfig(LogRotationConfig.getDefault());
    }
}
