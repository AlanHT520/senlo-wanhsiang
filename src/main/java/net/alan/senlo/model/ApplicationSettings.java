package net.alan.senlo.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.alan.senlo.config.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationSettings {
    public enum ConflictStrategy {
        RENAME, OVERWRITE, SKIP
    }

    private String outputDirectory = Config.DEFAULT_OUTPUT_DIR;
    private int maxConcurrentTasks = Config.MAX_CONCURRENT_TASKS;
    private String defaultOutputFormat = "mp4";
    private ConflictStrategy conflictStrategy = ConflictStrategy.RENAME;

    private String language = "en_us";      // 默认英语
    private boolean firstRun = true;        // 首次运行标志

    private static ApplicationSettings instance;
    private static final Path SETTINGS_FILE = Paths.get(System.getProperty("user.home"), ".senlo", "settings.json");

    private ApplicationSettings() {}

    public static ApplicationSettings getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ApplicationSettings load() {
        if (Files.exists(SETTINGS_FILE)) {
            try (Reader reader = Files.newBufferedReader(SETTINGS_FILE, java.nio.charset.StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                return gson.fromJson(reader, ApplicationSettings.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ApplicationSettings();
    }

    public void save() {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(SETTINGS_FILE, java.nio.charset.StandardCharsets.UTF_8)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // getters/setters
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isFirstRun() { return firstRun; }
    public void setFirstRun(boolean firstRun) { this.firstRun = firstRun; }

    public String getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }
    public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public void setMaxConcurrentTasks(int maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }
    public String getDefaultOutputFormat() { return defaultOutputFormat; }
    public void setDefaultOutputFormat(String defaultOutputFormat) { this.defaultOutputFormat = defaultOutputFormat; }
    public ConflictStrategy getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(ConflictStrategy conflictStrategy) { this.conflictStrategy = conflictStrategy; }
}