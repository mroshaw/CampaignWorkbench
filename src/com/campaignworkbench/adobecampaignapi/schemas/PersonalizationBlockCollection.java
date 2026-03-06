package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class PersonalizationBlockCollection extends CampaignSchema {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "includeView")
    private List<PersonalizationBlock> personalisationBlock;

    public PersonalizationBlockCollection() {
        super();
    }

    public List<PersonalizationBlock> getPersonalisationBlocks() {
        return personalisationBlock;
    }

    @Override
    public boolean isInitialized() {
        return personalisationBlock != null;
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
                    <orderBy>
                        <node expr="[folder/@fullName]"/>
                        <node expr="@label"/>
                    </orderBy>
                </queryDef>
                """;
    }

    public static String getUpdateXml(PersonalizationBlockKey key, String code) {
        return """
                <includeView xtkschema="nms:includeView" id="%s" _key="@id" >
                <source><text><![CDATA[%s]]></text></source>
                </includeView>
                """.formatted(key.getId(),code);
    }
}
