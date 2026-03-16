package com.campaignworkbench.adobecampaignapi;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;

import java.util.Optional;

public class CredentialStore {
    private static final String SERVICE = "campaign.workbench.oauth";

    private final Keyring keyring;
    private final String instanceId;

    public CredentialStore(String instanceId) {
        this.instanceId = instanceId;
        try {
            this.keyring = Keyring.create();
        } catch (BackendNotSupportedException backEndException) {
            throw new RuntimeException("Failed to create keyring", backEndException);
        }
    }

    private String key(String field) {
        return instanceId + "_" + field;
    }

    public void save(String clientId, String clientSecret, String endpointUrl) {
        try {
            keyring.setPassword(SERVICE, key("client_id"), clientId);
            keyring.setPassword(SERVICE, key("client_secret"), clientSecret);
            keyring.setPassword(SERVICE, key("endpoint_url"), endpointUrl);
        } catch (PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to save credentials", passwordAccessException);
        }
    }

    public Optional<String> getClientId() {
        try {
            return Optional.ofNullable(keyring.getPassword(SERVICE, key("client_id")));
        } catch (PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to get credentials", passwordAccessException);
        }
    }

    public Optional<String> getClientSecret() {
        try {
            return Optional.ofNullable(keyring.getPassword(SERVICE, key("client_secret")));
        } catch (PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to get credentials", passwordAccessException);
        }
    }

    public Optional<String> getEndpointUrl() {
        try {
            return Optional.ofNullable(keyring.getPassword(SERVICE, key("endpoint_url")));
        } catch (PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to get credentials", passwordAccessException);
        }
    }

    public void clear() {
        try {
            keyring.deletePassword(SERVICE, key("client_id"));
            keyring.deletePassword(SERVICE, key("client_secret"));
            keyring.deletePassword(SERVICE, key("endpoint_url"));
        } catch (PasswordAccessException passwordAccessException) {
            throw new RuntimeException("Failed to clear credentials", passwordAccessException);
        }
    }
}