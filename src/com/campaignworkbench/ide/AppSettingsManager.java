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
    private static final String settingsFileName = "app-settings.json";
    private static final Path settingsFilePath = AppSettings.appSettingsPath.resolve(settingsFileName);

    public AppSettingsManager(ErrorReporter errorReporter) {
        AppSettingsManager.errorReporter = errorReporter;
    }

    public static AppSettings load() {
        if (!Files.exists(settingsFilePath)) {
            return new AppSettings();
        }
        try {
            return JsonUtil.readFromJson(settingsFilePath, AppSettings.class);
        } catch (IOException ioe) {
            errorReporter.reportError("An error occurred loading app settings from path: " + settingsFilePath, ioe, true);
            return new AppSettings();
        }
    }

    public static void save(AppSettings settings) {

        try {
            // Ensure the parent directory exists (it should, since workspaces root is created at startup)
            Files.createDirectories(settingsFilePath.getParent());
            JsonUtil.writeToJson(settingsFilePath, settings);
        } catch (IOException ioe) {
            errorReporter.reportError("An error occurred saving app settings to path: " + settingsFilePath, ioe, true);
        }
    }
}