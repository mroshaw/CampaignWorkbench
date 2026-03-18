package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class EntitySchemasSchema extends CampaignSchema {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "schema")
    private List<EntitySchemaRecord> schemas;

    public List<EntitySchemaRecord> getSchemas() {
        return schemas;
    }

    public EntitySchemasSchema() {
        super();
    }

    public static String getQueryXml() {
        return """
                <queryDef schema="xtk:schema" operation="select">
                    <select>
                        <node expr="@namespace"/>
                        <node expr="@name"/>
                    </select>
                    <orderBy>
                        <node expr="@namespace"/>
                        <node expr="@name"/>
                    </orderBy>
                </queryDef>
                """;
    }
    @Override
    public boolean isInitialized() {
        return false;
    }
}
