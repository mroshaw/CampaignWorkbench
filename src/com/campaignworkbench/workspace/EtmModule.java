package com.campaignworkbench.workspace;

import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplateKey;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlockKey;
import com.campaignworkbench.adobecampaignapi.schemas.SchemaKey;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Class representing an ETM module workspace file.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class EtmModule extends WorkspaceContextFile {

    private JavaScriptTemplateKey schemaKey;

    public EtmModule(String fileName, Workspace workspace) {
        super(fileName, WorkspaceFileType.MODULE, workspace);
    }

    public EtmModule() {}

    @Override
    public void setKey(SchemaKey schemaKey) {
        this.schemaKey = (JavaScriptTemplateKey) schemaKey;
    }

    @Override
    public JavaScriptTemplateKey getKey() {
        return schemaKey;
    }

}
