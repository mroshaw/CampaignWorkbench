package com.campaignworkbench.adobecampaignapi.schemas;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class EtmModuleSchema extends CampaignSchema {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "jst")
    private List<EtmModuleRecord> javaScriptTemplates;

    public EtmModuleSchema() {
        super();
    }

    public List<EtmModuleRecord> getJavaScriptTemplates() {
        return javaScriptTemplates;
    }

    public void setJavaScriptTemplates(List<EtmModuleRecord> javaScriptTemplates) {
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

    public static String getUpdateXml(EtmModuleSchemaKey key, String code) {
        return """
                <jst xtkschema="xtk:jst" name="%s" namespace="%s" _key="@name,@namespace" >
                <code><![CDATA[%s]]></code>
                </jst>
                """.formatted(key.getName(), key.getNamespace(), code);
    }

    public static String getCreateXml(String label, String name, String nameSpace, String schema, String code) {
        return """
                <jst xtkschema="xtk:jst" label="%s" name="%s" namespace="%s" schema="%s" _key="@name,@namespace" >
                <code><![CDATA[%s]]></code>
                </jst>
                """.formatted(label, name, nameSpace, schema, code);
    }

    @Override
    public boolean isInitialized() {
        return javaScriptTemplates != null;
    }
}
