package com.campaignworkbench.workspace;

import com.campaignworkbench.adobecampaignapi.schemas.SchemaKey;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.time.LocalDateTime;

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

    private String originalFileName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime backupDate;

    public BackupFile(String fileName, String originalFileName, WorkspaceFileType sourceFileType, Workspace workspace) {
        super(fileName, WorkspaceFileType.BACKUP, workspace);
        this.sourceFileType = sourceFileType;
        this.originalFileName = originalFileName;
        backupDate = LocalDateTime.now();
    }

    public BackupFile() {}

    public WorkspaceFileType getSourceFileType() {
        return sourceFileType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public LocalDateTime getBackupDate() {
        return backupDate;
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