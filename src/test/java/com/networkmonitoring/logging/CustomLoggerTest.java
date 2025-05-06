package com.networkmonitoring.logging;

import com.networkmonitoring.config.LogRotationConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CustomLoggerTest {

    @TempDir
    Path tempLogDir;

    private CustomLogger logger;
    private LogRotationConfig config;
    private String logFileName = "test_app.log";

    @BeforeEach
    void setUp() {
        // Use a very small size for testing rotation quickly
        config = new LogRotationConfig(logFileName, 1, 3, tempLogDir.toString()); // 1MB, 3 backups
        logger = new CustomLogger(config);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (logger != null) {
            logger.close();
        }
        // Clean up temp directory files if any are left (though @TempDir should handle it)
        if (Files.exists(tempLogDir)) {
            try (Stream<Path> files = Files.list(tempLogDir)){
                 files.forEach(file -> {
                     try { Files.deleteIfExists(file); } catch (IOException e) { e.printStackTrace(); }
                 });
            }
        }
    }

    @Test
    void testLogCreation() throws IOException {
        String testMessage = "This is a test log message.";
        logger.info(testMessage, "TestClass");
        logger.close(); // Ensure flush and close

        Path logFilePath = tempLogDir.resolve(logFileName);
        assertTrue(Files.exists(logFilePath), "Log file should be created.");

        List<String> lines = Files.readAllLines(logFilePath);
        assertFalse(lines.isEmpty(), "Log file should not be empty.");
        assertTrue(lines.get(0).contains(testMessage), "Log file should contain the test message.");
        assertTrue(lines.get(0).contains(LogLevel.INFO.toString()), "Log entry should have INFO level.");
        assertTrue(lines.get(0).contains("[TestClass]"), "Log entry should contain the source.");
    }

    @Test
    void testLogRotation() throws IOException {
        // Configure for very small file size to trigger rotation quickly
        // 1KB = 1024 bytes. Let's make it even smaller for a single log line to trigger it.
        config = new LogRotationConfig(logFileName, (long)0.0001, 2, tempLogDir.toString()); // Approx 0.1KB
        logger.close(); // close previous logger
        logger = new CustomLogger(config);

        String baseMessage = "Logging to trigger rotation. Line number: ";
        int linesToLog = 10; // Log enough lines to surely exceed 0.1KB

        for (int i = 1; i <= linesToLog; i++) {
            logger.info(baseMessage + i, "RotationTest");
        }
        logger.close(); // Ensure rotation processing if pending

        // Check for backup files
        Path logDir = tempLogDir;
        File[] backupFiles = logDir.toFile().listFiles((dir, name) -> 
            name.startsWith(logFileName + ".") && name.matches(logFileName + "\\.\\d+")
        );

        assertNotNull(backupFiles, "Backup files array should not be null.");
        assertTrue(backupFiles.length > 0, "At least one backup file should exist after rotation.");
        assertTrue(backupFiles.length <= config.getMaxBackupFiles(), 
                   "Number of backup files should not exceed max backups: " + backupFiles.length + " vs " + config.getMaxBackupFiles());

        // Current log file should exist and be smaller than rotated ones (or new)
        Path currentLog = logDir.resolve(logFileName);
        assertTrue(Files.exists(currentLog), "Current log file should exist.");
        //assertTrue(Files.size(currentLog) < config.getMaxFileSizeBytes(), "Current log file should be small after rotation.");

        // Verify one of the backup files (e.g., .1)
        Path backup1 = logDir.resolve(logFileName + ".1");
        assertTrue(Files.exists(backup1), "Backup file log.1 should exist.");
        //assertTrue(Files.size(backup1) >= config.getMaxFileSizeBytes(), "Backup file should be around max size.");
    }

    @Test
    void testMaxBackupFilesLimit() throws IOException {
        int maxBackups = 2;
        config = new LogRotationConfig(logFileName, (long)0.0001, maxBackups, tempLogDir.toString()); // 0.1KB, 2 backups
        logger.close();
        logger = new CustomLogger(config);

        // Trigger rotation multiple times (maxBackups + 2 times to ensure oldest is deleted)
        for (int rotationCycle = 0; rotationCycle < maxBackups + 2; rotationCycle++) {
            // Log enough to trigger rotation
            for (int i = 0; i < 5; i++) { 
                logger.info("Cycle " + rotationCycle + " line " + i, "MaxBackupTest");
            }
            // Manually invoke rotation for test predictability if needed, though logging should trigger it.
            // For this test, we rely on the automatic checkAndRotate(). We need to close and reopen logger to ensure the previous file state is saved.
            logger.close();
            if (rotationCycle < maxBackups + 1) { // Don't reopen after the last cycle
                 logger = new CustomLogger(config); // Reopen to start with a fresh file check context
            }
        }
        // The last close is handled by @AfterEach

        File[] backupFiles = tempLogDir.toFile().listFiles((dir, name) -> name.startsWith(logFileName + "."));
        assertNotNull(backupFiles);
        
        List<String> backupFileNames = Stream.of(backupFiles)
                                            .map(File::getName)
                                            .filter(name -> name.matches(logFileName + "\\.\\d+"))
                                            .collect(Collectors.toList());
        
        assertEquals(maxBackups, backupFileNames.size(), 
                     "Number of backup files should be equal to maxBackupFiles setting. Found: " + backupFileNames);
        assertFalse(Files.exists(tempLogDir.resolve(logFileName + "." + (maxBackups + 1))), "Oldest backup should be deleted.");
    }

     @Test
    void testMultiThreadedLogging() throws InterruptedException {
        int numThreads = 5;
        int logsPerThread = 20;
        // Config for small files to ensure some rotation might occur, but primarily testing concurrent access
        config = new LogRotationConfig(logFileName, (long)0.001, 3, tempLogDir.toString()); // 1KB
        logger.close();
        logger = new CustomLogger(config);

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < logsPerThread; j++) {
                    logger.info("Log from thread " + threadId + ", message " + j, "ThreadedTest");
                    try {
                        Thread.sleep(1); // Small sleep to increase chance of interleaving
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        logger.close();

        Path logFilePath = tempLogDir.resolve(logFileName);
        assertTrue(Files.exists(logFilePath) || Files.exists(tempLogDir.resolve(logFileName + ".1")), 
                   "Log file or a backup should exist.");
        
        long totalLinesLogged = 0;
        if(Files.exists(logFilePath)) totalLinesLogged += Files.lines(logFilePath).count();
        
        File[] backupFiles = tempLogDir.toFile().listFiles((dir, name) -> name.startsWith(logFileName + "."));
        if (backupFiles != null) {
            for (File backupFile : backupFiles) {
                if (backupFile.getName().matches(logFileName + "\\.\\d+")) {
                    totalLinesLogged += Files.lines(backupFile.toPath()).count();
                }
            }
        }
        // Each thread logs 'logsPerThread' lines. 
        // This assertion can be tricky due to rotation creating new files. 
        // A better check might be for specific log contents if lines are unique.
        assertTrue(totalLinesLogged >= (long)numThreads * logsPerThread, 
                   "Total logged lines should be at least the number of messages sent. Expected approx: " + 
                   (numThreads * logsPerThread) + ", Got: " + totalLinesLogged);
    }
}
