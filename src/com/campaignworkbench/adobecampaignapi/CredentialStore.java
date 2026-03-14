package com.campaignworkbench.adobecampaignapi;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;

import java.util.Optional;

public class CredentialStore {
    private static final String SERVICE = "campaign.workbench.oauth";

    private final Keyring keyring;

    public CredentialStore() {
        try {
            this.keyring = Keyring.create();
        } catch (BackendNotSupportedException backEndException) {
            throw new RuntimeException("Failed to create keyring", backEndException);
        }
    }

    public void save(String instanceName, String clientId, String clientSecret, String endpointUrl) {
        try {
            keyring.setPassword(SERVICE, "client_id", clientId);
            keyring.setPassword(SERVICE, "client_secret", clientSecret);
            keyring.setPassword(SERVICE, "endpoint_url", endpointUrl);
        } catch (PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to save credentials", passwordAccessException);
        }
    }

    public Optional<String> getClientId() {
        try {
            return Optional.ofNullable(
                    keyring.getPassword(SERVICE, "client_id"));
        } catch(PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to get credentials", passwordAccessException);
        }
    }

    public Optional<String> getClientSecret() {
        try {
            return Optional.ofNullable(
                    keyring.getPassword(SERVICE, "client_secret"));
        } catch(PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to get credentials", passwordAccessException);
        }
    }

    public Optional<String> getEndpointUrl() {
        try {
            return Optional.ofNullable(
                    keyring.getPassword(SERVICE, "endpoint_url"));
        } catch(PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to get credentials", passwordAccessException);
        }
    }

    public void clear() {
        try {
            keyring.deletePassword(SERVICE, "client_id");
            keyring.deletePassword(SERVICE, "client_secret");
            keyring.deletePassword(SERVICE, "endpoint_url");
        } catch (PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to clear credentials", passwordAccessException);
        }
    }
}
