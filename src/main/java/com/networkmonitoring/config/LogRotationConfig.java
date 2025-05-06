package com.networkmonitoring.config;

/**
 * Configuration for log rotation.
 */
public class LogRotationConfig {
    private final String logFileName;
    private final long maxFileSizeMB;
    private final int maxBackupFiles;
    private final String logDirectory;

    // Defaults based on Project_Overview.md
    public static final String DEFAULT_LOG_FILE_NAME = "network_monitor.log";
    public static final long DEFAULT_MAX_FILE_SIZE_MB = 10; // 10MB
    public static final int DEFAULT_MAX_BACKUP_FILES = 5;
    public static final String DEFAULT_LOG_DIRECTORY = "logs";

    public LogRotationConfig(String logFileName, long maxFileSizeMB, int maxBackupFiles, String logDirectory) {
        this.logFileName = (logFileName == null || logFileName.trim().isEmpty()) ? DEFAULT_LOG_FILE_NAME : logFileName;
        this.maxFileSizeMB = (maxFileSizeMB <= 0) ? DEFAULT_MAX_FILE_SIZE_MB : maxFileSizeMB;
        this.maxBackupFiles = (maxBackupFiles < 0) ? DEFAULT_MAX_BACKUP_FILES : maxBackupFiles; // 0 means keep no backups
        this.logDirectory = (logDirectory == null || logDirectory.trim().isEmpty()) ? DEFAULT_LOG_DIRECTORY : logDirectory;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeMB * 1024 * 1024; // Convert MB to Bytes
    }
    
    public long getMaxFileSizeMB() {
        return maxFileSizeMB;
    }

    public int getMaxBackupFiles() {
        return maxBackupFiles;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public static LogRotationConfig getDefault() {
        return new LogRotationConfig(DEFAULT_LOG_FILE_NAME, DEFAULT_MAX_FILE_SIZE_MB, DEFAULT_MAX_BACKUP_FILES, DEFAULT_LOG_DIRECTORY);
    }
}
