package com.campaignworkbench.workspace;

/**
 * ENUM that contains meta-data about each type of workspace file.
 */
public enum WorkspaceFileType {
    TEMPLATE(
            "Templates",
            ".template",
            "Template File",
            "Template Files",
            "*.template"
    ),
    MODULE(
            "Modules",
            ".module",
            "Module File",
            "Module Files",
            "*.module"
    ),
    BLOCK(
            "Blocks",
            ".block",
            "Personalization Block File",
            "Block Files",
            "*.block"
    ),
    CONTEXT(
            "ContextXML",
            ".xml",
            "Contexts",
            "XML Files",
            "*.xml"
    ),
    BACKUP(
            "Backup",
            "",
            "Backup File",
            "Backup Files",
            "*.*"
    );

    // Used to derive paths
    private final String folderName;
    private final String fileExtension;

    // Used to determine text to use in an 'Open File' dialog window
    private final String fileOpenWindowTitle;
    private final String fileOpenExtensionFilterDescription;
    private final String fileOpenExtensionFilter;

    WorkspaceFileType(
            String folderName,
            String fileExtension,
            String fileOpenWindowTitle,
            String fileOpenExtensionFilterDescription,
            String fileOpenExtensionFilter) {

        this.folderName = folderName;
        this.fileExtension = fileExtension;
        this.fileOpenWindowTitle = fileOpenWindowTitle;
        this.fileOpenExtensionFilterDescription = fileOpenExtensionFilterDescription;
        this.fileOpenExtensionFilter = fileOpenExtensionFilter;
    }

    public String getFileOpenWindowTitle() {
        return fileOpenWindowTitle;
    }

    public String getFolderName() {
        return folderName;
    }

    public String extensionFilterDescription() {
        return fileOpenExtensionFilterDescription;
    }

    public String extensionFilter() {
        return fileOpenExtensionFilter;
    }

    public String getFileExtension() {
        return fileExtension;
    }

}
