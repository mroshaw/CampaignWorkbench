package com.campaignworkbench.adobecampaignapi.schemas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class PersoBlockSchemaKey extends SchemaKey {
    private long id;

    public PersoBlockSchemaKey(long id) {
        this.id = id;
    }

    // Need for JSON deserialization
    public PersoBlockSchemaKey() {}

    public long getId() {
        return id;
    }
}
