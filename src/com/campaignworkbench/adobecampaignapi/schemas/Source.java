package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Source {

    @JacksonXmlProperty(isAttribute = true)
    private boolean dependOnFormat;

    @JacksonXmlProperty(isAttribute = true)
    private boolean noEscaping;

    private String text;

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
