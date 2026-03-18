package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class EntitySchemaRecord {

    @JacksonXmlProperty(isAttribute = true)
    private String namespace;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
