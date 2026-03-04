package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class IncludeViewCollection extends CampaignSchema {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "includeView")
    private List<IncludeView> includeViews;

    public IncludeViewCollection() {
        super();
    }

    public List<IncludeView> getIncludeViews() {
        return includeViews;
    }

    @Override
    public boolean isInitialized() {
        return includeViews != null;
    }

    public static String getQueryXml() {
        return """
                    <queryDef schema="nms:includeView" operation="select">
                        <select>
                            <node expr="@id"/>
                            <node expr="@name"/>
                            <node expr="@label"/>
                            <node expr="data"/>
                              <node expr="[folder/@id]"/>
                              <node expr="[folder/@fullName]"/>
                        </select>
                    </queryDef>
                    """;
    }
}
