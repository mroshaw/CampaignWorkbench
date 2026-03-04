package com.campaignworkbench.adobecampaignapi.schemas;

public class JavaScriptTemplateIdentifier {
    private final String name;
    private final String namespace;

    public JavaScriptTemplateIdentifier(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
