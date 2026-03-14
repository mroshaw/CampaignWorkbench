package com.campaignworkbench.adobecampaignapi.schemas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class EtmModuleSchemaKey extends SchemaKey {
    private String name;
    private String namespace;

    public EtmModuleSchemaKey(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    // Need for JSON deserialization
    public EtmModuleSchemaKey() {}

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
