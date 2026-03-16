package com.campaignworkbench.ide;

import com.campaignworkbench.adobecampaignapi.CampaignInstance;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application-level settings persisted to app-settings.json.
 * Only non-sensitive data is stored here. Credentials remain in the system Keyring.
 */
public class AppSettings {

    @JsonProperty("instances")
    private List<CampaignInstance> instances = new ArrayList<>();

    public AppSettings() {}

    public List<CampaignInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<CampaignInstance> instances) {
        this.instances = instances;
    }

    public void addInstance(CampaignInstance instance) {
        instances.add(instance);
    }

    public void removeInstance(CampaignInstance instance) {
        instances.remove(instance);
    }

    public Optional<CampaignInstance> findById(String id) {
        return instances.stream()
                .filter(i -> i.getId().equals(id))
                .findFirst();
    }
}