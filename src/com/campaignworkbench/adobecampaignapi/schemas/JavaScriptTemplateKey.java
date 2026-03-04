package com.campaignworkbench.adobecampaignapi.schemas;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class JavaScriptTemplateKey extends SchemaKey {
    private String name;
    private String namespace;

    public JavaScriptTemplateKey(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    // Need for JSON deserialization
    public JavaScriptTemplateKey() {}

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
