package com.campaignworkbench.adobecampaignapi;

/**
 * High-level exception thrown by the Campaign API
 */
public class ApiException extends RuntimeException {
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
