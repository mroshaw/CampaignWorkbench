package com.campaignworkbench.workspace;

import com.campaignworkbench.adobecampaignapi.schemas.SchemaKey;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.nio.file.Path;

/**
 * Class representing a context XML workspace file.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class ContextXml extends WorkspaceFile {

    public ContextXml(String fileName, Workspace workspace) {
        super(fileName, WorkspaceFileType.CONTEXT, workspace);
    }

    public ContextXml() {}

    @Override
    public void setKey(SchemaKey schemaKey) {
    }

    @Override
    public SchemaKey getKey() {
        return null;
    }

}
