package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class JavaScriptTemplate implements ISchemaKey {

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String label;

    @JacksonXmlProperty(isAttribute = true)
    private String namespace;

    @JacksonXmlProperty(isAttribute = true)
    private String code;

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public JavaScriptTemplateKey getIdentifier() {
        return new JavaScriptTemplateKey(namespace, name);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public void print(boolean includeCode) {
        String output = "Namespace:" + namespace + " | " + "Name: " + name + " | " + "Label: " + label;
        if(includeCode) {
            output += " | " + "Code: " + code;
        }
        System.out.println(output);
    }

    @Override
    public SchemaKey getKey() {
        return new JavaScriptTemplateKey(name, namespace);
    }
}
