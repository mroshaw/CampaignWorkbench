package com.campaignworkbench.adobecampaignapi.schemas;

import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class FolderSchema extends CampaignSchema {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "folder")
    private List<FolderRecord> blockFolders;

    public List<FolderRecord> getBlockFolders() {
        return blockFolders;
    }

    public FolderSchema() {
        super();
    }

    public static String getQueryXml() {
        return """
                <queryDef schema="xtk:folder" operation="select">
                    <select>
                        <node expr="@id"/>
                        <node expr="@name"/>
                        <node expr="@fullName"/>
                        <node expr="@model"/>
                    </select>
                    <where>
                        <condition expr="@model= 'nmsIncludeView'"/>
                    </where>
                    <orderBy>
                        <node expr="@fullName"/>
                    </orderBy>
                </queryDef>
                """;
    }

    @Override
    public boolean isInitialized() {
        return blockFolders != null;
    }
}
