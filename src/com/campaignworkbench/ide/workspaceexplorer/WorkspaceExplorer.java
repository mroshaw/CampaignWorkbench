package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.ide.AppSettings;
import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.dialogs.NewWorkspaceDialog;
import com.campaignworkbench.ide.dialogs.YesNoCancelPopupDialog;
import com.campaignworkbench.ide.dialogs.YesNoPopupDialog;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.util.FileUtil;
import com.campaignworkbench.workspace.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * User interface control to explore and navigate the files in a workspace
 */
public class WorkspaceExplorer implements IJavaFxNode {

    // Observables
    private final StringProperty workspaceName = new SimpleStringProperty("No workspace selected");
    private final ObjectProperty<Workspace> workspace = new SimpleObjectProperty<>();

    // Change listeners
    private ListChangeListener<Template> templatesListener;
    private ListChangeListener<EtmModule> modulesListener;
    private ListChangeListener<PersoBlock> blocksListener;
    private ListChangeListener<ContextXml> contextsListener;

    // Root structures of the tree view
    private TreeView<Object> treeView;
    private TreeItem<Object> templateRoot;
    private TreeItem<Object> moduleRoot;
    private TreeItem<Object> blockRoot;
    private TreeItem<Object> contextRoot;

    // Event handlers
    private final Consumer<WorkspaceFile> fileOpenHandler;
    private final Consumer<String> insertIntoCodeHandler;

    // Toolbar
    private WorkspaceExplorerToolbar toolbar;

    // Main panel
    private VBox workspaceExplorerPanel;

    private WorkspaceFileType selectedFileType;
    private WorkspaceFile selectedFile;

    private final AppSettings appSettings;

    private final ErrorReporter errorReporter;

    private final CampaignOperationsHandler campaignOperationsHandler;


    /**
     * @param labelText           Label to use for the control in the UI
     * @param fileOpenHandler that handles double clicks of files in the Explorer
     */
    public WorkspaceExplorer(String labelText,
                             Consumer<WorkspaceFile> fileOpenHandler,
                             Consumer<Workspace> workspaceChangedHandler,
                             Consumer<String> insertIntoCodeHandler,
                             ErrorReporter errorReporter,
                             AppSettings appSettings) {

        this.fileOpenHandler = fileOpenHandler;
        this.insertIntoCodeHandler = insertIntoCodeHandler;
        this.errorReporter = errorReporter;
        this.appSettings = appSettings;

        campaignOperationsHandler = new CampaignOperationsHandler(
                errorReporter,
                fileOpenHandler,
                this::getWorkspace,
                () -> selectedFile,
                () -> selectedFileType,
                this::onCampaignConnectionStateChanged,
                this::getWindow,
                appSettings
        );

        createUi(labelText);
    }

    public Workspace getWorkspace() {
        return workspace.getValue();
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace.setValue(workspace);
        campaignOperationsHandler.onWorkspaceChanged(workspace);
        toolbar.update();
    }

    public boolean isWorkspaceOpen() {
        return workspace.getValue() != null;
    }

    private void bindWorkspace() {
        workspace.addListener((ObservableValue<? extends Workspace> _, Workspace oldWorkspace, Workspace newWorkspace) -> {

            // Remove old listeners
            if (oldWorkspace != null) {
                if (templatesListener != null)
                    oldWorkspace.getTemplates().removeListener(templatesListener);

                if (modulesListener != null)
                    oldWorkspace.getModules().removeListener(modulesListener);

                if (blocksListener != null)
                    oldWorkspace.getBlocks().removeListener(blocksListener);

                if (contextsListener != null)
                    oldWorkspace.getContexts().removeListener(contextsListener);
            }

            // Clear tree
            templateRoot.getChildren().clear();
            moduleRoot.getChildren().clear();
            blockRoot.getChildren().clear();
            contextRoot.getChildren().clear();
            workspaceName.unbind();

            if (newWorkspace == null) {
                workspaceName.set("No workspace selected");
            } else {
                workspaceName.bind(newWorkspace.getNameProperty());

                // Bind children to workspace.templates
                templatesListener = bindListToTree(newWorkspace.getTemplates(), templateRoot, template ->
                        WorkspaceExplorerItem.createTemplateTreeItem(template, this::deleteExistingFile), Comparator.comparing(Template::getBaseFileName)
                );

                modulesListener = bindListToTree(newWorkspace.getModules(), moduleRoot, module ->
                                WorkspaceExplorerItem.createModuleTreeItem(module, this::insertIntoCode, this::deleteExistingFile, this::restoreBackupHandler),
                        Comparator.comparing(EtmModule::getBaseFileName));

                blocksListener = bindListToTree(newWorkspace.getBlocks(), blockRoot, block ->
                                WorkspaceExplorerItem.createBlockTreeItem(block, this::insertIntoCode, this::deleteExistingFile, this::restoreBackupHandler),
                        Comparator.comparing(PersoBlock::getBaseFileName));

                contextsListener = bindListToTree(newWorkspace.getContexts(), contextRoot, context ->
                        WorkspaceExplorerItem.createContextTreeItem(context, this::deleteExistingFile), Comparator.comparing(ContextXml::getBaseFileName)
                );
            }
        });
    }

    private <T> ListChangeListener<T> bindListToTree(
            ObservableList<T> list,
            TreeItem<Object> parentRoot,
            Function<T, TreeItem<Object>> mapper,
            Comparator<T> sorter) {

        Comparator<TreeItem<Object>> treeItemSorter = (a, b) -> {
            T valA = (T) ((WorkspaceExplorerItem.WorkspaceFileTreeItem) a.getValue()).workspaceFile;
            T valB = (T) ((WorkspaceExplorerItem.WorkspaceFileTreeItem) b.getValue()).workspaceFile;
            return sorter.compare(valA, valB);
        };

        // Initial population
        parentRoot.getChildren().clear();
        for (T item : list) {
            parentRoot.getChildren().add(mapper.apply(item));
        }
        FXCollections.sort(parentRoot.getChildren(), treeItemSorter);

        ListChangeListener<T> listener = change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (T removed : change.getRemoved()) {
                        parentRoot.getChildren().removeIf(child ->
                                child.getValue() == removed ||
                                        (child.getValue() instanceof WorkspaceExplorerItem.WorkspaceFileTreeItem wft &&
                                                wft.workspaceFile == removed)
                        );
                    }
                }

                if (change.wasAdded()) {
                    for (T added : change.getAddedSubList()) {
                        parentRoot.getChildren().add(mapper.apply(added));
                    }
                    FXCollections.sort(parentRoot.getChildren(), treeItemSorter);
                }
            }
        };

        list.addListener(listener);
        return listener;
    }

    private void createUi(String labelText) {
        // Create the UI
        Label explorerLabel = new Label(labelText);
        explorerLabel.getStyleClass().add("ide-label");

        toolbar = new WorkspaceExplorerToolbar(
                campaignOperationsHandler,
                this::getWorkspace,
                () -> selectedFile,
                () -> selectedFileType,
                this::createNewHandler,
                this::addExistingHandler,
                this::deleteHandler,
                this::setDataContextHandler,
                this::clearDataContextHandler,
                this::setMessageContextHandler,
                this::clearMessageContextHandler
        );

        // TreeView for all items
        treeView = new TreeView<>();
        treeView.getStyleClass().add("workspace-explorer-treeview");
        // Bind the workspace to the TreeView
        bindWorkspace();

        // Create roots
        TreeItem<Object> workspaceRoot = WorkspaceExplorerItem.createHeaderTreeItemObservableText(IdeIcon.WORKSPACE, workspaceName,
                "workspace-icon", null, this::createNewFile, this::addExistingFile);

        templateRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(IdeIcon.TEMPLATE, "Templates",
                "template-icon", WorkspaceFileType.TEMPLATE, this::createNewFile, this::addExistingFile);

        moduleRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(IdeIcon.MODULE, "Modules",
                "module-icon", WorkspaceFileType.MODULE, this::createNewFile, this::addExistingFile);

        blockRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(IdeIcon.BLOCK, "Blocks",
                "block-icon", WorkspaceFileType.BLOCK, this::createNewFile, this::addExistingFile);

        contextRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(
                IdeIcon.CONTEXT, "Contexts",
                "context-icon", WorkspaceFileType.CONTEXT, this::createNewFile, this::addExistingFile);

        workspaceRoot.getChildren().setAll(templateRoot, moduleRoot, blockRoot, contextRoot);
        treeView.setRoot(workspaceRoot);
        workspaceRoot.setExpanded(true);

        // Apply the custom cell factory to render the tree nodes
        WorkspaceExplorerItem.applyCellFactory(treeView);

        // Create the main explorer container
        workspaceExplorerPanel = new VBox(explorerLabel, toolbar.getCampaignToolbar(), toolbar.getWorkspaceToolbar(), treeView);
        workspaceExplorerPanel.setMinHeight(0);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Listen for selection changes, so we can add context to the toolbar buttons
        treeView.getSelectionModel().selectedItemProperty().addListener(this::selectionChangedHandler);

        // Listen for double clicks
        setupDoubleClickHandler();

        // Set style classes
        workspaceExplorerPanel.getStyleClass().add("workspace-explorer");
        toolbar.update();
    }

    private void selectionChangedHandler(ObservableValue obs, TreeItem oldItem, TreeItem newItem) {
        if (newItem != null) {

            if (newItem.getValue() instanceof WorkspaceExplorerItem.ContextTreeItem) {
                TreeItem parentItem = newItem.getParent();

                if (parentItem != null && parentItem.getValue() instanceof WorkspaceExplorerItem.WorkspaceFileTreeItem parentWorkspaceItem) {
                    selectedFile = parentWorkspaceItem.workspaceFile;
                    selectedFileType = selectedFile.getFileType();
                }

                selectedFileType = selectedFile.getFileType();
            } else if (newItem.getValue() instanceof WorkspaceExplorerItem.WorkspaceFileTreeItem file) {
                selectedFile = file.workspaceFile;
                selectedFileType = selectedFile.getFileType();

            } else if (newItem.getValue() instanceof WorkspaceExplorerItem.HeaderTreeItem parentFolder) {
                selectedFileType = parentFolder.fileType;
                selectedFile = null;
            }
            toolbar.update();
        }
    }

    public void createNewWorkspace() {

        Optional<NewWorkspaceDialog.Result> result = NewWorkspaceDialog.show(getWindow(), appSettings);
        result.ifPresent(r -> {
            Workspace newWorkspace = new Workspace(r.workspaceName(), r.instance().getId(), true);
            newWorkspace.save();
            setWorkspace(newWorkspace);
        });
    }

    public void openWorkspace() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Workspace Folder");
        directoryChooser.setInitialDirectory(
                Workspace.getWorkspacesRootPath().toFile()
        );

        File folder = directoryChooser.showDialog(getWindow());

        if (folder == null) return;

        // Construct expected JSON file path
        String folderName = folder.toPath().getFileName().toString();
        Path expectedJsonFile = folder.toPath().resolve(folderName + ".json");

        if (Files.exists(expectedJsonFile) && Files.isRegularFile(expectedJsonFile)) {
            // System.out.println("File exists: " + expectedJsonFile);
            try {
                setWorkspace(new Workspace(folderName, null, false));
            } catch (WorkspaceException workspaceException) {
                errorReporter.reportError("Could not load workspace!", workspaceException, true);
            }
        } else {
            // Not a valid workspace
            errorReporter.reportError("The selected folder is not a valid workspace folder!", true);
        }

        toolbar.update();
    }

    public void saveWorkspace() {
        if (getWorkspace() != null) {
            try {
                getWorkspace().save();
            } catch (WorkspaceException workspaceException) {
                errorReporter.reportError("Could not save workspace!", workspaceException, true);
            }
        }
    }

    public void closeWorkspace() {

    }

    public void createNewFile(WorkspaceFileType workspaceFileType) {
        File selectedFile = FileUtil.createFile(getWorkspace(), workspaceFileType, getWindow());

        if (selectedFile == null) {
            return;
        }
        try {
            WorkspaceFile newFile = getWorkspace().createNewWorkspaceFile(selectedFile.getName(), workspaceFileType);
            fileOpenHandler.accept(newFile);
        } catch (WorkspaceException workspaceException) {
            errorReporter.reportError("Could not create new file!", workspaceException, true);
        }
    }

    public void addExistingFile(WorkspaceFileType workspaceFileType) {
        File selectedFile = FileUtil.openFile(getWorkspace(), workspaceFileType,  getWindow());

        if (selectedFile == null) {
            return;
        }
        try {
            getWorkspace().addWorkspaceFile(selectedFile.getName(), workspaceFileType);
        }
        catch (WorkspaceException workspaceException) {
            errorReporter.reportError("Could not add existing file!", workspaceException, true);
        }
    }

    private void deleteExistingFile(WorkspaceFile workspaceFile) {
        YesNoCancelPopupDialog.YesNoCancel result = YesNoCancelPopupDialog.show("Confirm delete?", "Do you also want to delete the selected file (" + selectedFile.getBaseFileName() + ") from the file system?", (Stage) getNode().getScene().getWindow());

        if (result == YesNoCancelPopupDialog.YesNoCancel.CANCEL) {
            return;
        }
        try {
            getWorkspace().removeWorkspaceFile(selectedFile, result == YesNoCancelPopupDialog.YesNoCancel.YES);
        } catch (WorkspaceException workspaceException) {
            errorReporter.reportError("An error occurred deleting " + selectedFile.getBaseFileName(), workspaceException, true);
        }
    }

    private void restoreBackupHandler(BackupFile backup) {
        String sourceBaseName = backup.getBaseFileName().replaceAll("_\\d{4}-\\d{2}-\\d{2}_\\d{6}$", "");
        WorkspaceFile targetFile = getWorkspace().getWorkspaceFile(sourceBaseName, backup.getSourceFileType());

        if (targetFile == null) {
            errorReporter.reportError("Could not find the source file to restore: " + sourceBaseName, true);
            return;
        }
        if (YesNoPopupDialog.show(
                "Confirm Restore",
                "Are you sure you want to restore " + targetFile.getFileName() + " from this backup? The current file content will be overwritten.",
                (Stage) getNode().getScene().getWindow()) == YesNoPopupDialog.YesNo.NO) {
            return;
        }
        try {
            getWorkspace().restoreBackup(backup, targetFile);
            fileOpenHandler.accept(targetFile);
            errorReporter.logMessage("Restored " + targetFile.getBaseFileName() + " from backup " + backup.getFileName());
        } catch (WorkspaceException e) {
            errorReporter.reportError("An error occurred restoring the backup for " + targetFile.getBaseFileName(), e, true);
        }
    }

    private void onCampaignConnectionStateChanged() {
        toolbar.onCampaignConnectionStateChanged();
    }

    private void insertIntoCode(String textToInsert) {
        insertIntoCodeHandler.accept(textToInsert);
    }

    private void createNewHandler() {

        if (selectedFileType == null) {
            return;
        }
        createNewFile(selectedFileType);
    }

    private void addExistingHandler() {
        if (selectedFileType == null) {
            return;
        }
        addExistingFile(selectedFileType);
    }

    private void deleteHandler() {
        if (selectedFile == null) {
            return;
        }
        deleteExistingFile(selectedFile);
    }

    private void setDataContextHandler() {

        if (selectedFile instanceof WorkspaceContextFile workspaceContextFile) {

            Optional<ContextXml> contextFile = WorkspaceFilePickerDialog.show(getWindow(), getWorkspace().getContexts());
            contextFile.ifPresent(workspaceContextFile::setDataContextFile);
        }
    }

    private void setMessageContextHandler() {
        if (selectedFile instanceof Template template) {

            Optional<ContextXml> contextFile = WorkspaceFilePickerDialog.show(getWindow(), getWorkspace().getContexts());
            contextFile.ifPresent(template::setMessageContextFile);
        }
    }

    private void clearDataContextHandler() {
        if (selectedFile instanceof WorkspaceContextFile workspaceFile) {
            workspaceFile.clearDataContext();
        }
    }

    private void clearMessageContextHandler() {
        if (selectedFile instanceof Template workspaceFile) {
            workspaceFile.clearMessageContext();
        }
    }

    private void setupDoubleClickHandler() {
        treeView.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, evt -> {
            if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {

                TreeItem<Object> selectedItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedItem == null) return;

                Object selectedObject = selectedItem.getValue();

                // Action depends on the type of the underlying item double-clicked

                // Folder header item
                if (selectedObject instanceof WorkspaceExplorerItem.HeaderTreeItem) {
                    // Allow the double click to fold/unfolder
                    return;
                }

                // Context child item
                if (selectedObject instanceof WorkspaceExplorerItem.ContextTreeItem contextTreeItem) {
                    if (contextTreeItem.workspaceFile == null) {
                        // Choose the context
                        if (contextTreeItem.contextLabel.startsWith("Data")) {
                            setDataContextHandler();
                        } else {
                            setMessageContextHandler();
                        }
                        return;
                    }

                    fileOpenHandler.accept(contextTreeItem.workspaceFile);
                    evt.consume();
                    return;
                }

                // File item
                if (selectedObject instanceof WorkspaceExplorerItem.WorkspaceFileTreeItem workspaceFileTreeItem) {
                    fileOpenHandler.accept(workspaceFileTreeItem.workspaceFile);
                    evt.consume();
                }
            }
        });
    }



    private Window getWindow() {
        return workspaceExplorerPanel.getScene().getWindow();
    }

    @Override
    public Node getNode() {
        return workspaceExplorerPanel;
    }
}
