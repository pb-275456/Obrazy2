package com.example.obrazy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.*;

public class Logs {
    private static final Logger logger = Logger.getLogger("AppLogger");

    static {
        try {
            Path logDir = Paths.get("logs");
            if (Files.notExists(logDir)) {
                Files.createDirectories(logDir);
            }

            FileHandler fileHandler = new FileHandler("logs/app-log.txt", true); //append do dodawania na koniec pliku
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Błąd przy tworzeniu loggera: " + e.getMessage());
        }
    }

    public static void logInfo(String message) {
        logger.info(message);
    }

    public static void logWarning(String message) {
        logger.warning(message);
    }

    public static void logError(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void logStartup() {
        logger.info("Aplikacja rozpoczęta.");
    }

    public static void logShutdown() {
        logger.info("Aplikacja zamnięta.");
    }
}