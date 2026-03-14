package com.campaignworkbench.ide.logging;

public interface ErrorReporter {
    void reportError(String message, boolean displayAlert);
    void reportError(String message, Throwable ex, boolean displayAlert);
    void logMessage(String message);
}