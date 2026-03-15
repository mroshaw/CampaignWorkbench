package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.workspace.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * Class to represent items in the Workspace Explorer TreeView.
 * Adds support for mixed content (text and icon) and workspace files.
 * Revised for TreeView virtualization safety.
 */
public class WorkspaceExplorerItem {

    abstract static class HeaderTreeItem {

        public final IdeIcon icon;
        public final String iconStyleClass;
        public final WorkspaceFileType fileType;

        protected HBox container;
        public final ContextMenu contextMenu;

        // Context menu handlers
        public final Consumer<WorkspaceFileType> addNewHandler;
        public final Consumer<WorkspaceFileType> addExistingHandler;

        private HeaderTreeItem(IdeIcon icon, String iconStyleClass, WorkspaceFileType fileType,
                               Consumer<WorkspaceFileType> addNewHandler, Consumer<WorkspaceFileType> addExistingHandler) {
            this.icon = icon;
            this.iconStyleClass = iconStyleClass;
            this.fileType = fileType;
            this.addNewHandler = addNewHandler;
            this.addExistingHandler = addExistingHandler;

            // Create ContextMenu
            this.contextMenu = new ContextMenu();
            if (fileType != null) {
                MenuItem addNew = new MenuItem("Add new...");
                addNew.setOnAction(_ -> addNewHandler(this));
                this.contextMenu.getItems().add(addNew);

                MenuItem addExisting = new MenuItem("Add existing...");
                addExisting.setOnAction(_ -> addExistingHandler(this));
                this.contextMenu.getItems().add(addExisting);
            }
        }
    }

    static class HeaderTreeItemStaticText extends HeaderTreeItem {

        private HeaderTreeItemStaticText(IdeIcon icon, String staticText, String iconStyleClass, WorkspaceFileType fileType, Consumer<WorkspaceFileType> addNewHandler, Consumer<WorkspaceFileType> addExistingHandler) {
            super(icon, iconStyleClass, fileType, addNewHandler, addExistingHandler);

            Node iconNode = icon.getIcon(20, iconStyleClass, true);
            Label labelPart = new Label(staticText);
            container = new HBox(iconNode, labelPart);
            this.container.getStyleClass().add("tree-item-container");
        }
    }

    static class HeaderTreeItemObservableText extends HeaderTreeItem {

        private HeaderTreeItemObservableText(IdeIcon icon, ObservableValue<String> observableText, String iconStyleClass, WorkspaceFileType fileType, Consumer<WorkspaceFileType> addNewHandler, Consumer<WorkspaceFileType> addExistingHandler) {
            super(icon, iconStyleClass, fileType, addNewHandler, addExistingHandler);

            // Create label
            Node iconNode = icon.getIcon(20, iconStyleClass, true);
            Text labelPart = new Text();
            if (observableText != null) {
                labelPart.textProperty().bind(observableText);
            }
            this.container = new HBox(iconNode, labelPart);
            this.container.getStyleClass().add("tree-item-container");
        }
    }

    static class WorkspaceFileTreeItem {

        public final WorkspaceFile workspaceFile;
        public final Consumer<String> getFileNameContextHandler;
        public final Consumer<WorkspaceFile> deleteHandler;

        public WorkspaceFileTreeItem(WorkspaceFile workspaceFile, Consumer<String> getFileNameContextHandler, Consumer<WorkspaceFile> deleteHandler) {
            this.workspaceFile = workspaceFile;
            this.getFileNameContextHandler = getFileNameContextHandler;
            this.deleteHandler = deleteHandler;
        }
    }

    static class ContextTreeItem {
        public final WorkspaceFile workspaceFile;
        public final String contextLabel;

        public ContextTreeItem(WorkspaceFile workspaceFile, String contextLabel) {
            this.workspaceFile = workspaceFile;
            this.contextLabel = contextLabel;
        }
    }

    static class BackupTreeItem {
        public final BackupFile backupFile;
        public final Consumer<BackupFile> restoreHandler;

        BackupTreeItem(BackupFile backupFile, Consumer<BackupFile> restoreHandler) {
            this.backupFile = backupFile;
            this.restoreHandler = restoreHandler;
        }
    }

    private static void setTreeItemValue(TreeItem<Object> item, Object newValue) {
        item.setValue(newValue);
        if (item.getParent() != null) {
            TreeItem<Object> parent = item.getParent();
            ObservableList<TreeItem<Object>> siblings = parent.getChildren();
            int idx = siblings.indexOf(item);
            siblings.set(idx, item);
        }
    }

    // TreeItem factories
    static TreeItem<Object> createTextTreeItem(String text) {
        return new TreeItem<>(text);
    }

    static TreeItem<Object> createWorkspaceFileTreeItem(WorkspaceFile workspaceFile,
                                                        Consumer<String> getFileNameContextHandler, Consumer<WorkspaceFile> deleteHandler) {
        WorkspaceFileTreeItem newTreeItem = new WorkspaceFileTreeItem(workspaceFile, getFileNameContextHandler, deleteHandler);
        return new TreeItem<>(newTreeItem);
    }

    static TreeItem<Object> createContextFileTreeItem(WorkspaceFile workspaceFile, String contextLabel) {
        ContextTreeItem newTreeItem = new ContextTreeItem(workspaceFile, contextLabel);
        return new TreeItem<>(newTreeItem);
    }

    static TreeItem<Object> createHeaderTreeItemStaticText(IdeIcon icon, String staticText, String iconStyleClass, WorkspaceFileType fileType,
                                                           Consumer<WorkspaceFileType> addNewHandler, Consumer<WorkspaceFileType> addExistingHandler) {
        HeaderTreeItem header = new HeaderTreeItemStaticText(icon, staticText, iconStyleClass, fileType, addNewHandler, addExistingHandler);
        return new TreeItem<>(header);
    }

    static TreeItem<Object> createHeaderTreeItemObservableText(IdeIcon icon, ObservableValue<String> observableText, String iconStyleClass, WorkspaceFileType fileType,
                                                               Consumer<WorkspaceFileType> addNewHandler, Consumer<WorkspaceFileType> addExistingHandler) {
        HeaderTreeItem header = new HeaderTreeItemObservableText(icon, observableText, iconStyleClass, fileType, addNewHandler, addExistingHandler);
        return new TreeItem<>(header);
    }

    static TreeItem<Object> createTemplateTreeItem(Template template, Consumer<WorkspaceFile> deleteHandler) {
        // Create parent TreeItem for the Template itself
        TreeItem<Object> templateItem = WorkspaceExplorerItem.createWorkspaceFileTreeItem(
                template,
                text -> {
                }, // Optional: context text handler
                deleteHandler
        );

        // Add dataContext child
        TreeItem<Object> dataContextItem = WorkspaceExplorerItem.createContextFileTreeItem(
                template.getDataContextFile(),
                "Data"
        );

        // Add messageContext child
        TreeItem<Object> messageContextItem = WorkspaceExplorerItem.createContextFileTreeItem(
                template.getMessageContextFile(),
                "Message"
        );

        templateItem.getChildren().addAll(dataContextItem, messageContextItem);

        // Bind child items to the Template properties
        template.getDataContextFileProperty().addListener((obs, oldFile, newFile) -> {
            setTreeItemValue(dataContextItem, new ContextTreeItem(newFile, "Data"));
        });

        template.getMessageContextFileProperty().addListener((obs, oldFile, newFile) -> {
            setTreeItemValue(messageContextItem, new ContextTreeItem(newFile, "Message"));
        });

        return templateItem;
    }

    static TreeItem<Object> createModuleTreeItem(EtmModule etmModule, Consumer<String> getFileNameContextHandler, Consumer<WorkspaceFile> deleteHandler, Consumer<BackupFile> restoreHandler) {
        // Create parent TreeItem for the Template itself
        TreeItem<Object> moduleItem = WorkspaceExplorerItem.createWorkspaceFileTreeItem(
                etmModule,
                getFileNameContextHandler,
                deleteHandler
        );

        // Add dataContext child
        TreeItem<Object> dataContextItem = WorkspaceExplorerItem.createContextFileTreeItem(
                etmModule.getDataContextFile(),
                "Data"
        );

        moduleItem.getChildren().addAll(dataContextItem);
        // templateItem.setExpanded(true); // optional: expand by default

        // Bind child items to the Template properties
        etmModule.getDataContextFileProperty().addListener((obs, oldFile, newFile) -> {
            setTreeItemValue(dataContextItem, new ContextTreeItem(newFile, "Data"));
        });

        TreeItem<Object> backupsHeaderItem = createBackupsHeaderTreeItem(etmModule.getBackups(), restoreHandler);
        moduleItem.getChildren().addAll(dataContextItem, backupsHeaderItem);

        return moduleItem;
    }

    static TreeItem<Object> createBlockTreeItem(PersoBlock block, Consumer<String> getFileNameContextHandler, Consumer<WorkspaceFile> deleteHandler, Consumer<BackupFile> restoreHandler) {
        // Create parent TreeItem for the Template itself
        TreeItem<Object> blockItem = WorkspaceExplorerItem.createWorkspaceFileTreeItem(
                block,
                getFileNameContextHandler,
                deleteHandler
        );

        TreeItem<Object> backupsHeaderItem = createBackupsHeaderTreeItem(block.getBackups(), restoreHandler);
        blockItem.getChildren().addAll(backupsHeaderItem);

        return blockItem;
    }

    static TreeItem<Object> createContextTreeItem(ContextXml context, Consumer<WorkspaceFile> deleteHandler) {
        // Create parent TreeItem for the Template itself
        TreeItem<Object> contextItem = WorkspaceExplorerItem.createWorkspaceFileTreeItem(
                context,
                text -> {
                }, // Optional: context text handler
                deleteHandler
        );

        return contextItem;
    }


    // Cell Factory
    static void applyCellFactory(TreeView<Object> treeView) {
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                // Reset cell state first (important for recycled cells)
                setText(null);
                setGraphic(null);
                setContextMenu(null);

                if (empty || item == null) {
                    return;
                }

                // Reset class
                getStyleClass().removeAll("workspace-file", "workspace-file-linked");

                switch (item) {
                    // Item header has an icon graphic and a context menu
                    case HeaderTreeItem iconText -> {
                        setGraphic(iconText.container);
                        setContextMenu(iconText.contextMenu);
                    }
                    case ContextTreeItem contextTreeItem -> {
                        if (contextTreeItem.workspaceFile != null) {
                            setText(contextTreeItem.contextLabel + ": " + contextTreeItem.workspaceFile.getBaseFileName());
                        } else {
                            setText(contextTreeItem.contextLabel + ": (NOT SET)");
                        }
                        setContextMenu(null);
                    }
                    // Workspace file has name of the file and a context menu
                    case WorkspaceFileTreeItem workspaceFileTreeItem -> {
                        setText(workspaceFileTreeItem.workspaceFile.getBaseFileName());

                        if(workspaceFileTreeItem.workspaceFile.hasCampaignKey()) {
                            getStyleClass().add("workspace-file-linked");
                        } else {
                            getStyleClass().add("workspace-file");
                        }

                        WorkspaceFileType fileType = workspaceFileTreeItem.workspaceFile.getFileType();
                        ContextMenu menu = new ContextMenu();

                        if (fileType == WorkspaceFileType.BLOCK || fileType == WorkspaceFileType.MODULE) {
                            MenuItem insertIntoCode = new MenuItem("Insert into code...");
                            insertIntoCode.setOnAction(e -> insertIntoCodeHandler(workspaceFileTreeItem));
                            menu.getItems().add(insertIntoCode);
                            menu.getItems().add(new SeparatorMenuItem());
                            setContextMenu(menu);
                        }
                        MenuItem delete = new MenuItem("Delete...");
                        delete.setOnAction(e -> deleteFileEventHandler(workspaceFileTreeItem));
                        menu.getItems().add(delete);
                        setContextMenu(menu);
                    }
                    case BackupTreeItem backupTreeItem -> {
                        setText(backupTreeItem.backupFile.getBaseFileName());
                        getStyleClass().add("backup-file");
                        ContextMenu menu = new ContextMenu();
                        MenuItem restore = new MenuItem("Restore...");
                        restore.setOnAction(_ -> backupTreeItem.restoreHandler.accept(backupTreeItem.backupFile));
                        menu.getItems().add(restore);
                        setContextMenu(menu);
                    }
                    case String s when s.equals("Backups") -> {
                        setText("Backups");
                        setGraphic(null);
                        setContextMenu(null);
                    }
                    // Default is just a text item
                    default -> {
                        setText(item.toString());
                        setGraphic(null);
                        setContextMenu(null);
                    }
                }
            }
        });
    }

    private static TreeItem<Object> createBackupsHeaderTreeItem(
            ObservableList<BackupFile> backups,
            Consumer<BackupFile> restoreHandler) {

        TreeItem<Object> backupsRoot = new TreeItem<>("Backups");

        for (BackupFile backup : backups) {
            backupsRoot.getChildren().add(new TreeItem<>(new BackupTreeItem(backup, restoreHandler)));
        }

        backups.addListener((ListChangeListener<BackupFile>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (BackupFile added : change.getAddedSubList()) {
                        backupsRoot.getChildren().add(0, new TreeItem<>(new BackupTreeItem(added, restoreHandler)));
                    }
                }
                if (change.wasRemoved()) {
                    backupsRoot.getChildren().removeIf(child ->
                            child.getValue() instanceof BackupTreeItem bt &&
                                    change.getRemoved().contains(bt.backupFile));
                }
            }
        });

        return backupsRoot;
    }

    // Handlers for context menu items
    private static void addNewHandler(HeaderTreeItem headerTreeItem) {
        headerTreeItem.addNewHandler.accept(headerTreeItem.fileType);
    }

    private static void addExistingHandler(HeaderTreeItem headerTreeItem) {
        headerTreeItem.addExistingHandler.accept(headerTreeItem.fileType);
    }

    private static void insertIntoCodeHandler(WorkspaceFileTreeItem workspaceFileTreeItem) {
        String includeType = workspaceFileTreeItem.workspaceFile.getFileType() == WorkspaceFileType.BLOCK ? "view" : "module";
        String fileName = workspaceFileTreeItem.workspaceFile.getBaseFileName();
        String text = "<%@ include " + includeType + "='" + fileName + "' %>";
        workspaceFileTreeItem.getFileNameContextHandler.accept(text);
    }

    private static void deleteFileEventHandler(WorkspaceFileTreeItem workspaceFileTreeItem) {
        workspaceFileTreeItem.deleteHandler.accept(workspaceFileTreeItem.workspaceFile);
    }
}