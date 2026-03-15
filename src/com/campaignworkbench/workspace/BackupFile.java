package com.campaignworkbench.workspace;

import com.campaignworkbench.adobecampaignapi.schemas.SchemaKey;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;

/**
 * Represents a timestamped backup of an EtmModule or PersoBlock, taken before a push to Campaign.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class BackupFile extends WorkspaceFile {

    // The file type of the source file this backs up (MODULE or BLOCK)
    private WorkspaceFileType sourceFileType;

    public BackupFile(String fileName, WorkspaceFileType sourceFileType, Workspace workspace) {
        super(fileName, WorkspaceFileType.BACKUP, workspace);
        this.sourceFileType = sourceFileType;
    }

    public BackupFile() {}

    public WorkspaceFileType getSourceFileType() {
        return sourceFileType;
    }

    /**
     * Resolves into e.g. Modules/Backups/ or Blocks/Backups/
     */
    @JsonIgnore
    @Override
    public Path getAbsoluteFilePath() {
        return getWorkspace().getRootFolderPath()
                .resolve(sourceFileType.getFolderName())
                .resolve(WorkspaceFileType.BACKUP.getFolderName())
                .resolve(getFileName());
    }

    @Override
    public void setKey(SchemaKey schemaKey) {
        // Backups have no Campaign key
    }

    @Override
    public SchemaKey getKey() {
        return null;
    }
}