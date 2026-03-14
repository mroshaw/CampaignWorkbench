package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class FolderRecord {
    @JacksonXmlProperty(isAttribute = true)
    private long id;

    @JacksonXmlProperty(isAttribute = true)
    private String fullName;

    public void setId(long id) {
        this.id = id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }
}
