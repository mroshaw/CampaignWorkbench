package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplateKey;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlock;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlockKey;
import com.campaignworkbench.ide.*;
import com.campaignworkbench.ide.TextInputDialog;
import com.campaignworkbench.util.FileUtil;
import com.campaignworkbench.util.UiUtil;
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
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.glyphfont.FontAwesome;

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

    // Context menu labels
    private static final String createNewButtonText = "Create new";
    private static final String addExistingButtonText = "Add existing";
    private static final String removeButtonText = "Remove";

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
    private TreeItem<Object> workspaceRoot;
    private TreeItem<Object> templateRoot;
    private TreeItem<Object> moduleRoot;
    private TreeItem<Object> blockRoot;
    private TreeItem<Object> contextRoot;

    // Event handlers
    private final Consumer<WorkspaceFile> fileOpenHandler;
    private final Consumer<Workspace> workspaceChangedHandler;
    private final Consumer<String> insertIntoCodeHandler;

    // Toolbar buttons
    private Button createNewButton;
    private Button addExistingButton;
    private Button removeButton;
    private Button setDataContextButton;
    private Button clearDataContextButton;
    private Button setMessageContextButton;
    private Button clearMessageContextButton;
    private Button connectToCampaignButton;
    private Button disconnectFromCampaignButton;
    private Button createNewFromCampaignButton;
    private Button refreshFromCampaignButton;
    private Button pushToCampaignButton;

    // Main panel
    private VBox workspaceExplorerPanel;

    private WorkspaceFileType selectedFileType;
    private WorkspaceFile selectedFile;
    private WorkspaceFile selectedContextFile;

    private final ErrorReporter errorReporter;

    private boolean isConnectedToCampaign;

    /**
     * @param labelText           Label to use for the control in the UI
     * @param fileOpenHandler that handles double clicks of files in the Explorer
     */
    public WorkspaceExplorer(String labelText,
                             Consumer<WorkspaceFile> fileOpenHandler,
                             Consumer<Workspace> workspaceChangedHandler,
                             Consumer<String> insertIntoCodeHandler, ErrorReporter errorReporter) {

        this.fileOpenHandler = fileOpenHandler;
        this.workspaceChangedHandler = workspaceChangedHandler;
        this.insertIntoCodeHandler = insertIntoCodeHandler;
        this.errorReporter = errorReporter;
        createUi(labelText);
    }

    public Workspace getWorkspace() {
        return workspace.getValue();
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace.setValue(workspace);
        setToolbarButtonStates();
    }

    public boolean isWorkspaceOpen() {
        return workspace.getValue() != null;
    }

    private void bindWorkspace() {
        workspace.addListener((obs, oldWorkspace, newWorkspace) -> {

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

                // Sort children
                // System.out.println("Sorting templates");
                FXCollections.sort(newWorkspace.getTemplates(), Comparator.comparing(Template::getBaseFileName));
                // Bind children to workspace.templates
                templatesListener = bindListToTree(newWorkspace.getTemplates(), templateRoot, template ->
                        WorkspaceExplorerItem.createTemplateTreeItem(template, this::deleteExistingFile), Comparator.comparing(Template::getBaseFileName)
                );

                // System.out.println("Sorting blocks");
                FXCollections.sort(newWorkspace.getBlocks(), Comparator.comparing(PersoBlock::getBaseFileName));
                modulesListener = bindListToTree(newWorkspace.getModules(), moduleRoot, module ->
                        WorkspaceExplorerItem.createModuleTreeItem(module, this::insertIntoCode, this::deleteExistingFile), Comparator.comparing(EtmModule::getBaseFileName)
                );

                // System.out.println("Sorting modules");
                FXCollections.sort(newWorkspace.getModules(), Comparator.comparing(EtmModule::getBaseFileName));
                blocksListener = bindListToTree(newWorkspace.getBlocks(), blockRoot, block ->
                        WorkspaceExplorerItem.createBlockTreeItem(block, this::insertIntoCode, this::deleteExistingFile), Comparator.comparing(PersoBlock::getBaseFileName)
                );

                // System.out.println("Sorting contexts");
                FXCollections.sort(newWorkspace.getContexts(), Comparator.comparing(ContextXml::getBaseFileName));
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

        // Initial population
        parentRoot.getChildren().clear();
        for (T item : list) {
            parentRoot.getChildren().add(mapper.apply(item));
        }

        // Sort initial children
        sortTreeItems(parentRoot, sorter);

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

        // Mini toolbar
        createNewButton = UiUtil.createButton("", "Create new", FontAwesome.Glyph.FILE, "neutral-icon", 1, true, _ -> createNewHandler());
        addExistingButton = UiUtil.createButton("", "Add existing", FontAwesome.Glyph.PLUS_CIRCLE, "positive-icon", 1, true, _ -> addExistingHandler());
        removeButton = UiUtil.createButton("", "Remove", FontAwesome.Glyph.MINUS_CIRCLE, "negative-icon", 1, true, _ -> deleteHandler());
        setDataContextButton = UiUtil.createButton("", "Set Data Context", FontAwesome.Glyph.FILE_CODE_ALT, "positive-icon", 1, true, _ -> setDataContextHandler());
        clearDataContextButton = UiUtil.createButton("", "Clear Data Context", FontAwesome.Glyph.FILE_CODE_ALT, "negative-icon", 1, true, _ -> clearDataContextHandler());
        setMessageContextButton = UiUtil.createButton("", "Set Message Context", FontAwesome.Glyph.ENVELOPE, "positive-icon", 1, true, _ -> setMessageContextHandler());
        clearMessageContextButton = UiUtil.createButton("", "Clear Message Context", FontAwesome.Glyph.ENVELOPE, "negative-icon", 1, true, _ -> clearMessageContextHandler());

        // Campaign connection toolbar buttons
        connectToCampaignButton = UiUtil.createButton("", "Connect to Campaign", FontAwesome.Glyph.CIRCLE, "positive-icon", 1, true, _ -> connectToCampaignHandler());
        disconnectFromCampaignButton = UiUtil.createButton("", "Disconnect from Campaign", FontAwesome.Glyph.CIRCLE_ALT, "negative-icon", 1, false, _ -> disconnectFromCampaignHandler());
        createNewFromCampaignButton = UiUtil.createButton("", "Create new from Campaign", FontAwesome.Glyph.FILE, "positive-icon", 1, false, _ -> createNewFromCampaignHandler());
        refreshFromCampaignButton = UiUtil.createButton("", "Refresh from Campaign", FontAwesome.Glyph.ARROW_CIRCLE_LEFT, "neutral-icon", 1, false, _ -> refreshFromCampaignHandler());
        pushToCampaignButton = UiUtil.createButton("", "Push to Campaign", FontAwesome.Glyph.ARROW_CIRCLE_RIGHT, "neutral-icon", 1, false, _ -> pushToCampaignHandler());

        ToolBar workspaceToolbar = new ToolBar(createNewButton, addExistingButton, removeButton, setDataContextButton, clearDataContextButton, setMessageContextButton, clearMessageContextButton);
        ToolBar campaignToolbar = new ToolBar(connectToCampaignButton, disconnectFromCampaignButton, createNewFromCampaignButton, refreshFromCampaignButton, pushToCampaignButton);

        // TreeView for all items
        treeView = new TreeView<>();
        treeView.getStyleClass().add("workspace-explorer-treeview");
        // Bind the workspace to the TreeView
        bindWorkspace();

        // Create roots
        workspaceRoot = WorkspaceExplorerItem.createHeaderTreeItemObservableText(FontAwesome.Glyph.FOLDER, workspaceName,
                "workspace-icon", null, this::createNewFile, this::addExistingFile);

        templateRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(FontAwesome.Glyph.ENVELOPE, "Templates",
                "template-icon", WorkspaceFileType.TEMPLATE, this::createNewFile, this::addExistingFile);

        moduleRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(FontAwesome.Glyph.TASKS, "Modules",
                "module-icon", WorkspaceFileType.MODULE, this::createNewFile, this::addExistingFile);

        blockRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(FontAwesome.Glyph.LIST, "Blocks",
                "block-icon", WorkspaceFileType.BLOCK, this::createNewFile, this::addExistingFile);

        contextRoot = WorkspaceExplorerItem.createHeaderTreeItemStaticText(
                FontAwesome.Glyph.FILE_CODE_ALT, "Contexts",
                "context-icon", WorkspaceFileType.CONTEXT, this::createNewFile, this::addExistingFile);

        workspaceRoot.getChildren().setAll(templateRoot, moduleRoot, blockRoot, contextRoot);
        treeView.setRoot(workspaceRoot);
        workspaceRoot.setExpanded(true);

        // Apply the custom cell factory to render the tree nodes
        WorkspaceExplorerItem.applyCellFactory(treeView);

        // Create the main explorer container
        workspaceExplorerPanel = new VBox(explorerLabel, campaignToolbar, workspaceToolbar, treeView);
        workspaceExplorerPanel.setMinHeight(0);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Listen for selection changes, so we can add context to the toolbar buttons
        treeView.getSelectionModel().selectedItemProperty().addListener(this::selectionChangedHandler);

        // Listen for double clicks
        setupDoubleClickHandler();

        // Set style classes
        workspaceExplorerPanel.getStyleClass().add("workspace-explorer");
        workspaceToolbar.getStyleClass().add("small-toolbar");
        campaignToolbar.getStyleClass().add("small-toolbar");

        setToolbarButtonStates();
    }

    private void selectionChangedHandler(ObservableValue obs, TreeItem oldItem, TreeItem newItem) {
        if (newItem != null) {

            if (newItem.getValue() instanceof WorkspaceExplorerItem.ContextTreeItem contextTreeItem) {
                selectedContextFile = contextTreeItem.workspaceFile;

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
                selectedContextFile = null;
            }
            setToolbarButtonStates();
        }
    }

    private void setToolbarButtonStates() {

        if(workspace.getValue() == null) {
            setDataContextButton.setDisable(true);
            clearDataContextButton.setDisable(true);
            setMessageContextButton.setDisable(true);
            clearMessageContextButton.setDisable(true);
            addExistingButton.setDisable(true);
            createNewButton.setDisable(true);
            removeButton.setDisable(true);
            pushToCampaignButton.setDisable(true);
            refreshFromCampaignButton.setDisable(true);
            createNewFromCampaignButton.setDisable(true);
            connectToCampaignButton.setDisable(true);
            disconnectFromCampaignButton.setDisable(true);
            return;
        }

        connectToCampaignButton.setDisable(isConnectedToCampaign);
        disconnectFromCampaignButton.setDisable(!isConnectedToCampaign);

        if (selectedFile == null) {
            setDataContextButton.setDisable(true);
            clearDataContextButton.setDisable(true);
            setMessageContextButton.setDisable(true);
            clearMessageContextButton.setDisable(true);
        }

        if (selectedFileType == null) {
            addExistingButton.setDisable(true);
            createNewButton.setDisable(true);
            removeButton.setDisable(true);
            return;
        }

        createNewButton.setTooltip(new Tooltip(createNewButtonText + " " + selectedFileType.toString().toLowerCase()));
        addExistingButton.setTooltip(new Tooltip(addExistingButtonText + " " + selectedFileType.toString().toLowerCase()));
        removeButton.setTooltip(new Tooltip(removeButtonText + " " + selectedFileType.toString().toLowerCase()));

        boolean isWorkspaceFileSelected = selectedFile != null;
        boolean isTemplateSelected = selectedFileType == WorkspaceFileType.TEMPLATE && isWorkspaceFileSelected;
        boolean isModuleSelected = selectedFileType == WorkspaceFileType.MODULE && isWorkspaceFileSelected;
        boolean isBlockSelected = selectedFileType == WorkspaceFileType.BLOCK && isWorkspaceFileSelected;

        addExistingButton.setDisable(false);
        createNewButton.setDisable(false);
        removeButton.setDisable(!isWorkspaceFileSelected);

        switch (selectedFileType) {
            case TEMPLATE:
                pushToCampaignButton.setDisable(!(isTemplateSelected && isConnectedToCampaign));

                setDataContextButton.setDisable(!isTemplateSelected);
                clearDataContextButton.setDisable(!isTemplateSelected);
                setMessageContextButton.setDisable(!isTemplateSelected);
                clearMessageContextButton.setDisable(!isTemplateSelected);
                pushToCampaignButton.setDisable(!isConnectedToCampaign);
                break;
            case MODULE:
                createNewFromCampaignButton.setDisable(!isConnectedToCampaign);
                pushToCampaignButton.setDisable(!(isWorkspaceFileSelected && isConnectedToCampaign && selectedFile.hasCampaignKey()));
                refreshFromCampaignButton.setDisable(!(isWorkspaceFileSelected && isConnectedToCampaign && selectedFile.hasCampaignKey()));
                setDataContextButton.setDisable(!isModuleSelected);
                clearDataContextButton.setDisable(!isModuleSelected);
                setMessageContextButton.setDisable(true);
                clearMessageContextButton.setDisable(true);
                break;
            case BLOCK:
                createNewFromCampaignButton.setDisable(!isConnectedToCampaign);
                pushToCampaignButton.setDisable(!(isWorkspaceFileSelected && isConnectedToCampaign && selectedFile.hasCampaignKey()));
                refreshFromCampaignButton.setDisable(!(isWorkspaceFileSelected && isConnectedToCampaign && selectedFile.hasCampaignKey()));
                setDataContextButton.setDisable(true);
                clearDataContextButton.setDisable(true);
                setMessageContextButton.setDisable(true);
                clearMessageContextButton.setDisable(true);
                break;
            case CONTEXT:
            default:
                pushToCampaignButton.setDisable(true);
                refreshFromCampaignButton.setDisable(true);
                createNewFromCampaignButton.setDisable(true);
                setDataContextButton.setDisable(true);
                clearDataContextButton.setDisable(true);
                setMessageContextButton.setDisable(true);
                clearMessageContextButton.setDisable(true);
                break;
        }
    }

    public void createNewWorkspace() {

        Optional<String> result = TextInputDialog.show(getWindow(), "Create workspace", "Please enter a unique name for the new workspace", "Workspace name:");

        result.ifPresent(workspaceName -> {
            // Only executed if OK was clicked and text was not empty
            System.out.println("Workspace Name: " + workspaceName);

            setWorkspace(new Workspace(workspaceName, true));
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
            System.out.println("File exists: " + expectedJsonFile);
            setWorkspace(new Workspace(folderName, false));
            getWorkspace().load();
        } else {
            // Not a valid workspace
            errorReporter.reportError("The selected folder is not a valid workspace folder!", true);
        }

        setToolbarButtonStates();
    }

    public void saveWorkspace() {
        if (getWorkspace() != null) {
            try {
                getWorkspace().save();
            } catch (IdeException ideException) {
                throw new IdeException("Could not save workspace!", ideException.getCause());
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

        WorkspaceFile newFile = getWorkspace().createNewWorkspaceFile(selectedFile.getName(), workspaceFileType);
        fileOpenHandler.accept(newFile);
    }

    public void addExistingFile(WorkspaceFileType workspaceFileType) {
        File selectedFile = FileUtil.openFile(getWorkspace(), workspaceFileType,  getWindow());

        if (selectedFile == null) {
            return;
        }

        getWorkspace().addWorkspaceFile(selectedFile.getName(), workspaceFileType);
    }

    private void deleteExistingFile(WorkspaceFile workspaceFile) {
        YesNoPopupDialog.YesNoCancel result = YesNoPopupDialog.show("Confirm delete?", "Do you also want to delete the selected file (" + selectedFile.getBaseFileName() + ") from the file system?", (Stage) getNode().getScene().getWindow());

        if (result == YesNoPopupDialog.YesNoCancel.CANCEL) {
            return;
        }
        getWorkspace().removeWorkspaceFile(selectedFile, result == YesNoPopupDialog.YesNoCancel.YES);
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

    private void connectToCampaignHandler() {
        try {
            CampaignServerManager.connect();
        } catch (IdeException ideException) {
            errorReporter.reportError("An error occurred connecting to Adobe Campaign. Please check File > Settings!", true);
            return;
        }
        errorReporter.logMessage("Connected to Campaign server at: " + CampaignServerManager.getEndpointUrl());
        isConnectedToCampaign = true;
        setToolbarButtonStates();
    }

    private void disconnectFromCampaignHandler() {
        CampaignServerManager.disconnect();
        errorReporter.logMessage("Disconnected from Campaign at: " + CampaignServerManager.getEndpointUrl());
        isConnectedToCampaign = false;
        setToolbarButtonStates();
    }

    private void createNewFromCampaignHandler() {
        if(selectedFileType == WorkspaceFileType.BLOCK) {
            Optional<PersonalizationBlock> newBlock = CampaignBlockPickerDialog.show(getWindow());
            if(newBlock.isEmpty()) {
                return;
            }
            PersonalizationBlockKey key = (PersonalizationBlockKey)newBlock.get().getKey();
            System.out.println("Create new " + selectedFileType + " from " + newBlock.get().getLabel() + " with key " + key.getId());

            WorkspaceFile newFile = workspace.get().createNewWorkspaceFile(newBlock.get().getName(), WorkspaceFileType.BLOCK, newBlock.get().getCode(), key);
            newFile.setKey(key);
            fileOpenHandler.accept(newFile);
        }

        if(selectedFileType == WorkspaceFileType.MODULE) {
            Optional<JavaScriptTemplate> newModule = CampaignModulePickerDialog.show(getWindow());
            if(newModule.isEmpty()) {
                return;
            }
            JavaScriptTemplateKey key = (JavaScriptTemplateKey)newModule.get().getKey();
            System.out.println("Create new " + selectedFileType + " from " + newModule.get().getLabel() + " with key (" + key.getNamespace() + ":" + key.getName() + ")");

            WorkspaceFile newFile = workspace.get().createNewWorkspaceFile(newModule.get().getName(), WorkspaceFileType.MODULE, newModule.get().getCode(), key);

            fileOpenHandler.accept(newFile);
        }
    }

    private void refreshFromCampaignHandler() {

        if(selectedFileType == WorkspaceFileType.BLOCK) {
            CampaignServerManager.refreshBlocks();
            PersonalizationBlockKey key = (PersonalizationBlockKey) selectedFile.getKey();
            Optional<PersonalizationBlock> block = CampaignServerManager.getPersonalizationBlock(key.getId());
            if(block.isPresent()) {
                selectedFile.saveWorkspaceFileContent(block.get().getCode());
                fileOpenHandler.accept(selectedFile);
            }
        }

        if(selectedFileType == WorkspaceFileType.MODULE) {
            CampaignServerManager.refreshJavaScriptTemplates();
            JavaScriptTemplateKey key = (JavaScriptTemplateKey) selectedFile.getKey();
            Optional<JavaScriptTemplate> javascriptTemplate = CampaignServerManager.getJavaScriptTemplate(key.getNamespace(), key.getName());
            if(javascriptTemplate.isPresent()) {
                selectedFile.saveWorkspaceFileContent(javascriptTemplate.get().getCode());
                fileOpenHandler.accept(selectedFile);
            }
        }
    }

    private void pushToCampaignHandler() {

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

                    // Call open file on the selected context file
                    WorkspaceFile fileToOpen= contextTreeItem.workspaceFile;
                    // fileToOpen.setWorkspace(workspace);

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

    private <T> void sortTreeItems(TreeItem<Object> parentRoot, Comparator<T> sorter) {
        FXCollections.sort(parentRoot.getChildren(), (a, b) -> {
            T valA = (T) ((WorkspaceExplorerItem.WorkspaceFileTreeItem) a.getValue()).workspaceFile;
            T valB = (T) ((WorkspaceExplorerItem.WorkspaceFileTreeItem) b.getValue()).workspaceFile;
            return sorter.compare(valA, valB);
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
