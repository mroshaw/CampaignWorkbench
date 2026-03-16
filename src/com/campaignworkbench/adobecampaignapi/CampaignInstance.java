package com.campaignworkbench.adobecampaignapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents a named Adobe Campaign server instance.
 * Only the id and name are persisted to JSON. Credentials are always
 * stored exclusively in the system Keyring via CredentialStore.
 */
public class CampaignInstance {

    private final String id;
    private String name;

    @JsonIgnore
    private final CredentialStore credentialStore;

    /**
     * Creates a new CampaignInstance with a generated UUID.
     */
    public CampaignInstance(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.credentialStore = new CredentialStore(this.id);
    }

    /**
     * Jackson deserialization constructor — reconstructs from persisted id and name.
     */
    @JsonCreator
    public CampaignInstance(@JsonProperty("id") String id,
                            @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
        this.credentialStore = new CredentialStore(this.id);
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
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