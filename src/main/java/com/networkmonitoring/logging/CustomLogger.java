package com.networkmonitoring.logging;

import com.networkmonitoring.config.LogRotationConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Custom logger with log rotation capabilities.
 * Implements thread-safe logging and rotation.
 */
public class CustomLogger {
    private final LogRotationConfig config;
    private Path currentLogFilePath;
    private BufferedWriter writer;
    private final ReentrantLock lock = new ReentrantLock(); // For thread-safety

    public CustomLogger(LogRotationConfig config) {
        this.config = config;
        initializeLogger();
    }

    private void initializeLogger() {
        try {
            Path logDir = Paths.get(config.getLogDirectory());
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            this.currentLogFilePath = logDir.resolve(config.getLogFileName());
            this.writer = new BufferedWriter(new FileWriter(currentLogFilePath.toFile(), true)); // Append mode
        } catch (IOException e) {
            System.err.println("Error initializing logger: " + e.getMessage());
            // Fallback to console if file logger fails
            this.writer = null; 
        }
    }

    public void log(LogLevel level, String message, String source) {
        log(new LogEntry(level, message, source));
    }

    public void log(LogEntry entry) {
        lock.lock();
        try {
            checkAndRotate();
            if (writer != null) {
                writer.write(entry.toString());
                writer.newLine();
                writer.flush();
            } else {
                // Fallback to console output if writer is not initialized
                System.out.println(entry.toString());
            }
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void checkAndRotate() throws IOException {
        if (writer == null || !Files.exists(currentLogFilePath)) return; // Can't rotate if no file

        long fileSize = Files.size(currentLogFilePath);
        if (fileSize >= config.getMaxFileSizeBytes()) {
            rotate();
        }
    }

    private void rotate() throws IOException {
        if (writer != null) {
            writer.close();
        }

        Path logDir = Paths.get(config.getLogDirectory());
        String baseFileName = config.getLogFileName();

        // Delete oldest backup if maxBackupFiles is exceeded
        if (config.getMaxBackupFiles() > 0) {
            File[] existingBackups = logDir.toFile().listFiles(
                (dir, name) -> name.startsWith(baseFileName + ".") && name.matches(baseFileName + "\\.\\d+")
            );
            if (existingBackups != null && existingBackups.length >= config.getMaxBackupFiles()) {
                Arrays.sort(existingBackups, Comparator.comparingInt(f -> Integer.parseInt(f.getName().substring(baseFileName.length() + 1))));
                // Delete the oldest, which is the first one after numeric sort
                Files.deleteIfExists(existingBackups[0].toPath()); 
            }
        }
        
        // Shift existing backup files: log.3 -> log.4, log.2 -> log.3, log.1 -> log.2
        for (int i = config.getMaxBackupFiles() -1 ; i >= 1; i--) {
            Path oldBackup = logDir.resolve(baseFileName + "." + i);
            Path newBackup = logDir.resolve(baseFileName + "." + (i + 1));
            if (Files.exists(oldBackup)) {
                Files.move(oldBackup, newBackup, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Rotate current log to log.1
        Path firstBackup = logDir.resolve(baseFileName + ".1");
        if(Files.exists(currentLogFilePath)){
             Files.move(currentLogFilePath, firstBackup, StandardCopyOption.REPLACE_EXISTING);
        }
       
        // Reinitialize writer for the new current log file
        this.writer = new BufferedWriter(new FileWriter(currentLogFilePath.toFile(), false)); // New file, not append
        System.out.println("Log rotated: " + currentLogFilePath);
    }

    // Convenience methods
    public void info(String message, String source) {
        log(LogLevel.INFO, message, source);
    }

    public void warn(String message, String source) {
        log(LogLevel.WARNING, message, source);
    }

    public void error(String message, String source) {
        log(LogLevel.ERROR, message, source);
    }
    
    public void debug(String message, String source) {
        log(LogLevel.DEBUG, message, source);
    }

    public void close() {
        lock.lock();
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            System.err.println("Error closing logger: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
    
    // For testing purposes
    Path getCurrentLogFilePath() {
        return currentLogFilePath;
    }
}
