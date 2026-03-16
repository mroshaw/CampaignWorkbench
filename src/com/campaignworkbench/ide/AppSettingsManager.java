package com.campaignworkbench.ide;

import com.campaignworkbench.util.JsonUtil;
import com.campaignworkbench.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages loading and saving of application-level settings (app-settings.json).
 * The settings file is stored alongside workspaces in the Campaign Workbench root directory.
 */
public class AppSettingsManager {

    private static final String SETTINGS_FILE_NAME = "app-settings.json";

    private static Path getSettingsFilePath() {
        return Workspace.getWorkspacesRootPath().resolve(SETTINGS_FILE_NAME);
    }

    public static AppSettings load() {
        Path path = getSettingsFilePath();
        if (!Files.exists(path)) {
            return new AppSettings();
        }
        try {
            return JsonUtil.readFromJson(path, AppSettings.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load app settings from: " + path, e);
        }
    }

    public static void save(AppSettings settings) {
        Path path = getSettingsFilePath();
        try {
            // Ensure the parent directory exists (it should, since workspaces root is created at startup)
            Files.createDirectories(path.getParent());
            JsonUtil.writeToJson(path, settings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save app settings to: " + path, e);
        }
    }
}