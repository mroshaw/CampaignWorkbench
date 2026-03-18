package com.campaignworkbench.ide;

import com.campaignworkbench.ide.logging.ErrorReporter;
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

    private static ErrorReporter errorReporter;
    private static final String SETTINGS_FILE_NAME = "app-settings.json";

    public AppSettingsManager(ErrorReporter errorReporter) {
        AppSettingsManager.errorReporter = errorReporter;
    }

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
        } catch (IOException ioe) {
            errorReporter.reportError("An error occurred loading app settings from path: " + path, ioe, true);
            return new AppSettings();
        }
    }

    public static void save(AppSettings settings) {
        Path path = getSettingsFilePath();
        try {
            // Ensure the parent directory exists (it should, since workspaces root is created at startup)
            Files.createDirectories(path.getParent());
            JsonUtil.writeToJson(path, settings);
        } catch (IOException ioe) {
            errorReporter.reportError("An error occurred saving app settings to path: " + path, ioe, true);
        }
    }
}