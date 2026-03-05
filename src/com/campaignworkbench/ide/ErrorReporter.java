package com.campaignworkbench.ide;

public interface ErrorReporter {
    void reportError(String message, boolean displayAlert);
    void reportError(String message, Throwable ex, boolean displayAlert);
    void logMessage(String message);
}