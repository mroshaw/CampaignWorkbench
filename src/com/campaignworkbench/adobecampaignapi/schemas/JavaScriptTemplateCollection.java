package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class JavaScriptTemplateCollection extends CampaignSchema {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "jst")
    private List<JavaScriptTemplate> javaScriptTemplates;

    public JavaScriptTemplateCollection() {
        super();
    }

    public List<JavaScriptTemplate> getJavaScriptTemplates() {
        return javaScriptTemplates;
    }

    public void setJavaScriptTemplates(List<JavaScriptTemplate> javaScriptTemplates) {
        this.javaScriptTemplates = javaScriptTemplates;
    }

    public static String getQueryXml() {
        return """
                <queryDef schema="xtk:jst" operation="select">
                    <select>
                        <node expr="@name"/>
                        <node expr="@label"/>
                        <node expr="@namespace"/>
                        <node expr="code"/>
                    </select>
                    <orderBy>
                        <node expr="@name"/>
                    </orderBy>
                </queryDef>
                """;
    }

    public static String getUpdateXml(JavaScriptTemplateKey key, String code) {
        return """
                <jst xtkschema="xtk:jst" name="%s" namespace="%s" _key="@name,@namespace" >
                <code><![CDATA[%s]]></code>
                </jst>
                """.formatted(key.getName(), key.getNamespace(), code);
    }

    @Override
    public boolean isInitialized() {
        return javaScriptTemplates != null;
    }
}
