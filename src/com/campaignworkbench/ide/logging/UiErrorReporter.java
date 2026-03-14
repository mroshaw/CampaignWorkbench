package com.campaignworkbench.ide.logging;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class UiErrorReporter implements ErrorReporter {

    private final LogPanel logPanel;
    private final ErrorLogPanel errorLogPanel;

    public UiErrorReporter(LogPanel logPanel, ErrorLogPanel errorLogPanel) {
        this.logPanel = logPanel;
        this.errorLogPanel = errorLogPanel;
    }

    @Override
    public void reportError(String message, boolean displayAlert) {
        Platform.runLater(() -> {
            logPanel.addLogEntry(message);

            if (displayAlert) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Application Error");
                alert.setContentText(message);
                alert.showAndWait();
            }
        });
    }

    @Override
    public void reportError(String message, Throwable ex, boolean displayAlert) {
        errorLogPanel.addError((Exception) ex);
        reportError(message + ": " + ex.getMessage(), displayAlert);
    }

    @Override
    public void logMessage(String message) {
        logPanel.addLogEntry(message);
    }
}