package com.campaignworkbench.workspace;

import com.campaignworkbench.adobecampaignapi.schemas.EtmModuleSchemaKey;
import com.campaignworkbench.adobecampaignapi.schemas.SchemaKey;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

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

    private EtmModuleSchemaKey schemaKey;
    @JsonIgnore
    private final ObservableList<BackupFile> backups = FXCollections.observableArrayList();

    @JsonProperty("backups")
    private List<BackupFile> getBackupsForJson() {
        return new ArrayList<>(backups);
    }

    @JsonProperty("backups")
    private void setBackupsForJson(List<BackupFile> list) {
        if (list != null) backups.setAll(list);
    }

    public ObservableList<BackupFile> getBackups() {
        return backups;
    }

    public EtmModule(String fileName, Workspace workspace) {
        super(fileName, WorkspaceFileType.MODULE, workspace);
    }

    public EtmModule() {}

    @Override
    public void setKey(SchemaKey schemaKey) {
        this.schemaKey = (EtmModuleSchemaKey) schemaKey;
    }

    @Override
    public EtmModuleSchemaKey getKey() {
        return schemaKey;
    }

}
