package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PersonalizationBlock implements ISchemaKey {
    @JacksonXmlProperty(isAttribute = true)
    private long id;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String label;

    // Elements
    private Source source;
    private Folder folder;

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public void setSource(Source source) {
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

    public Folder getFolder() {
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
        return new PersonalizationBlockKey(id);
    }
}
