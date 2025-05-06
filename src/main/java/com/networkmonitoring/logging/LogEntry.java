package com.networkmonitoring.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single log entry.
 */
public class LogEntry {
    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String message;
    private final String source; // e.g., class name or component

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LogEntry(LogLevel level, String message, String source) {
        this.timestamp = LocalDateTime.now();
        this.level = level;
        this.message = message;
        this.source = source;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] [%s] - %s", 
                             timestamp.format(formatter),
                             level,
                             source,
                             message);
    }
}
