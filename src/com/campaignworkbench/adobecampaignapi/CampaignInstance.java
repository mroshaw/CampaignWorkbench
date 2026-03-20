package com.campaignworkbench.adobecampaignapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Represents a named Adobe Campaign server instance.
 * Only the id and name are persisted to JSON. Credentials are always
 * stored exclusively in the system Keyring via CredentialStore.
 */
public class CampaignInstance {

    private final String id;
    private String name;
    private final String endpointUrl;
    private static final String userHomeFolderName = "Campaign Workbench";
    public static final Path userHome =  Paths.get(System.getProperty("user.home")).resolve(userHomeFolderName);

    @JsonIgnore
    private final CredentialStore credentialStore;

    /**
     * Creates a new CampaignInstance with a generated UUID.
     */
    public CampaignInstance(String name, String endpointUrl) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.endpointUrl = endpointUrl;
        credentialStore = new CredentialStore(id);
    }

    /**
     * Jackson deserialization constructor — reconstructs from persisted id and name.
     */
    @JsonCreator
    public CampaignInstance(@JsonProperty("id") String id,
                            @JsonProperty("name") String name,
                            @JsonProperty("url") String endpointUrl) {
        this.id = id;
        this.name = name;
        this.endpointUrl = endpointUrl;
        credentialStore = new CredentialStore(id);
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("url")
    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public CredentialStore getCredentialStore() {
        return credentialStore;
    }

    @Override
    public String toString() {
        return name;
    }
    }