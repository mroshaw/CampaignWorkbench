package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PersoBlockRecord implements ISchemaKey {
    @JacksonXmlProperty(isAttribute = true)
    private long id;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String label;

    // Elements
    private SourceRecord source;
    private FolderRecord folder;

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setFolder(FolderRecord folder) {
        this.folder = folder;
    }

    public void setSource(SourceRecord source) {
        this.source = source;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return source.getText();
    }

    public FolderRecord getFolder() {
        return folder;
    }

    public void print(boolean includeCode) {
        String output = "Id:" + id + " | " + "Name: " + name + " | " + "Label: " + label + " | " + "Folder: " + folder.getFullName();
        if(includeCode) {
            output += " | " + "Code: " + getCode();
        }
        System.out.println(output);
    }

    @Override
    public SchemaKey getKey() {
        return new PersoBlockSchemaKey(id);
    }
}
