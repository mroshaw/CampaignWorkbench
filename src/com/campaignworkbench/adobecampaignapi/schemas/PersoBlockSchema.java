package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class PersoBlockSchema extends CampaignSchema {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "includeView")
    private List<PersoBlockRecord> personalisationBlock;

    public PersoBlockSchema() {
        super();
    }

    public List<PersoBlockRecord> getPersonalisationBlocks() {
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

    public static String getUpdateXml(PersoBlockSchemaKey key, String code) {
        return """
                <includeView xtkschema="nms:includeView" id="%s" _key="@id" >
                <source><text><![CDATA[%s]]></text></source>
                </includeView>
                """.formatted(key.getId(),code);
    }

    public static String getCreateXml(String internalName, String label, long folderId, String code) {
        return """
                <includeView xtkschema="nms:includeView" name="%s"  label="%s" _key="@name" >
                <source><text><![CDATA[%s]]></text></source>
                <folder id="%s"/>
                </includeView>
                """.formatted(internalName ,label,code, folderId);
    }
}
