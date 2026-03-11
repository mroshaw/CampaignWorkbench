package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.ApiException;
import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplateKey;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlock;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlockKey;
import com.campaignworkbench.ide.*;
import com.campaignworkbench.ide.TextInputDialog;
import com.campaignworkbench.ide.dialogs.YesNoCancelPopupDialog;
import com.campaignworkbench.ide.dialogs.YesNoPopupDialog;
import com.campaignworkbench.ide.icons.IdeIcon;
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

import java.io.File;
import java.net.URI;
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
    private TreeItem<Object> templateRoot;
    private TreeItem<Object> moduleRoot;
    private TreeItem<Object> blockRoot;
    private TreeItem<Object> contextRoot;

    // Event handlers
    private final Consumer<WorkspaceFile> fileOpenHandler;
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

    private Label urlHostLabel;

    // Main panel
    private VBox workspaceExplorerPanel;

    private WorkspaceFileType selectedFileType;
    private WorkspaceFile selectedFile;

    private final ErrorReporter errorReporter;
    private final CampaignServerManager campaignServerManager;
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
        this.insertIntoCodeHandler = insertIntoCodeHandler;
        this.errorReporter = errorReporter;
        campaignServerManager = new CampaignServerManager();
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
        createNewButton = UiUtil.createButton("", "Create new", IdeIcon.NEW_FILE, true, "neutral-icon", 20, 16,true, _ -> createNewHandler());
        addExistingButton = UiUtil.createButton("", "Add existing",IdeIcon.ADD_FILE, true, "positive-icon", 20, 16, true, _ -> addExistingHandler());
        removeButton = UiUtil.createButton("", "Remove", IdeIcon.DELETE_FILE, true,"negative-icon", 20, 16, true, _ -> deleteHandler());
        setDataContextButton = UiUtil.createButton("", "Set Data Context", IdeIcon.SET_DATA_CONTEXT, true, "positive-icon", 20, 16,true, _ -> setDataContextHandler());
        clearDataContextButton = UiUtil.createButton("", "Clear Data Context", IdeIcon.CLEAR_DATA_CONTEXT, true, "negative-icon", 20, 16,true, _ -> clearDataContextHandler());
        setMessageContextButton = UiUtil.createButton("", "Set Message Context", IdeIcon.SET_MESSAGE_CONTEXT, true, "positive-icon", 20, 16,true, _ -> setMessageContextHandler());
        clearMessageContextButton = UiUtil.createButton("", "Clear Message Context", IdeIcon.CLEAR_MESSAGE_CONTEXT, true, "negative-icon", 20, 16, true, _ -> clearMessageContextHandler());

        // Campaign connection toolbar buttons
        connectToCampaignButton = UiUtil.createButton("", "Connect to Campaign", IdeIcon.CONNECT,  true,"positive-icon", 20, 16,true, _ -> connectToCampaignHandler());
        disconnectFromCampaignButton = UiUtil.createButton("", "Disconnect from Campaign", IdeIcon.DISCONNECT, true,"negative-icon", 20, 16,false, _ -> disconnectFromCampaignHandler());
        createNewFromCampaignButton = UiUtil.createButton("", "Create new from Campaign", IdeIcon.NEW_FROM_CAMPAIGN,  true,"positive-icon", 20, 16,false, _ -> createNewFromCampaignHandler());
        refreshFromCampaignButton = UiUtil.createButton("", "Refresh from Campaign", IdeIcon.REFRESH_FROM_CAMPAIGN, true, "neutral-icon", 20, 16,false, _ -> refreshFromCampaignHandler());
        pushToCampaignButton = UiUtil.createButton("", "Push to Campaign", IdeIcon.UPDATE_TO_CAMPAIGN, true, "neutral-icon", 20, 16,false, _ -> pushToCampaignHandler());
        urlHostLabel = new Label();

        ToolBar workspaceToolbar = new ToolBar(createNewButton, addExistingButton, removeButton, setDataContextButton, clearDataContextButton, setMessageContextButton, clearMessageContextButton);
        ToolBar campaignToolbar = new ToolBar(connectToCampaignButton, disconnectFromCampaignButton, createNewFromCampaignButton, refreshFromCampaignButton, pushToCampaignButton, urlHostLabel);

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
            // System.out.println("File exists: " + expectedJsonFile);
            try {
                setWorkspace(new Workspace(folderName, false));
                getWorkspace().load();
            } catch (WorkspaceException workspaceException) {
                errorReporter.reportError("Could not load workspace!", workspaceException, true);
            }
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
            campaignServerManager.connect();
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred connecting to Adobe Campaign. Please check File > Settings!", true);
            return;
        }
        errorReporter.logMessage("Connected to Campaign server at: " + campaignServerManager.getEndpointUrl());
        isConnectedToCampaign = true;
        String endPointUrl = campaignServerManager.getEndpointUrl();
        String host = URI.create(endPointUrl).getHost(); // "specsavers-mkt-stage14.campaign.adobe.com"
        String hostname = host.split("\\.")[0];  // "specsavers-mkt-stage14"
        urlHostLabel.setText(hostname);
        setToolbarButtonStates();
    }

    private void disconnectFromCampaignHandler() {
        try {
            campaignServerManager.disconnect();
            errorReporter.logMessage("Disconnected from Campaign at: " + campaignServerManager.getEndpointUrl());
            isConnectedToCampaign = false;
            setToolbarButtonStates();
            urlHostLabel.setText("");
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred disconnecting from Adobe Campaign", apiException, true);
        }
    }

    private void createNewFromCampaignHandler() {
        if(selectedFileType == WorkspaceFileType.BLOCK) {
            Optional<PersonalizationBlock> newBlock = CampaignBlockPickerDialog.show(getWindow(), campaignServerManager);
            if(newBlock.isEmpty()) {
                return;
            }

            PersonalizationBlockKey key = (PersonalizationBlockKey)newBlock.get().getKey();
            // System.out.println("Create new " + selectedFileType + " from " + newBlock.get().getLabel() + " with key " + key.getId());

            errorReporter.logMessage("Creating " + selectedFileType + " from Adobe Campaign using " + newBlock.get().getLabel() + " with key " + key.getId() + ". Please wait...");
            WorkspaceFile newFile = workspace.get().createNewWorkspaceFile(newBlock.get().getName(), WorkspaceFileType.BLOCK, newBlock.get().getCode(), key);
            newFile.setKey(key);
            fileOpenHandler.accept(newFile);
        }

        if(selectedFileType == WorkspaceFileType.MODULE) {
            Optional<JavaScriptTemplate> newModule = CampaignModulePickerDialog.show(getWindow(), campaignServerManager);
            if(newModule.isEmpty()) {
                return;
            }
            JavaScriptTemplateKey key = (JavaScriptTemplateKey)newModule.get().getKey();
            System.out.println("Create new " + selectedFileType + " from " + newModule.get().getLabel() + " with key (" + key.getNamespace() + ":" + key.getName() + ")");

            WorkspaceFile newFile = workspace.get().createNewWorkspaceFile(newModule.get().getName(), WorkspaceFileType.MODULE, newModule.get().getCode(), key);

            fileOpenHandler.accept(newFile);
        }
        errorReporter.logMessage("Created new " + selectedFileType + " from Adobe Campaign");
    }

    private void refreshFromCampaignHandler() {
        if(!selectedFile.hasCampaignKey())
        {
            errorReporter.reportError("The selected file does not have a Campaign key!", true);
            return;
        }

        if(YesNoPopupDialog.show("Are you sure?", "Are you sure you want to refresh " + selectedFileType + " " + selectedFile.getFileName() + " from Adobe Campaign?", (Stage) getNode().getScene().getWindow()) == YesNoPopupDialog.YesNo.NO) {
            return;
        }


        errorReporter.logMessage("Refreshing " + selectedFile.getBaseFileName() + " from Adobe Campaign. Please wait...");
        try {
            if (selectedFileType == WorkspaceFileType.BLOCK) {
                campaignServerManager.refreshBlocks();
                PersonalizationBlockKey key = (PersonalizationBlockKey) selectedFile.getKey();
                Optional<PersonalizationBlock> block = campaignServerManager.getPersonalizationBlock(key.getId());
                if (block.isPresent()) {
                    selectedFile.saveWorkspaceFileContent(block.get().getCode());
                    fileOpenHandler.accept(selectedFile);
                }
            }

            if (selectedFileType == WorkspaceFileType.MODULE) {
                campaignServerManager.refreshJavaScriptTemplates();
                JavaScriptTemplateKey key = (JavaScriptTemplateKey) selectedFile.getKey();
                Optional<JavaScriptTemplate> javascriptTemplate = campaignServerManager.getJavaScriptTemplate(key.getNamespace(), key.getName());
                if (javascriptTemplate.isPresent()) {
                    selectedFile.saveWorkspaceFileContent(javascriptTemplate.get().getCode());
                    fileOpenHandler.accept(selectedFile);
                }
            }
            errorReporter.logMessage("Refreshed " + selectedFileType + " " + selectedFile.getBaseFileName() + " from Adobe Campaign");
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred refreshing " + selectedFile.getBaseFileName() + " from Adobe Campaign", apiException, true);
        }
    }

    private void pushToCampaignHandler() {
        if(!selectedFile.hasCampaignKey())
        {
            errorReporter.reportError("The selected file does not have a Campaign key!", true);
            return;
        }

        if(YesNoPopupDialog.show("Are you sure?", "Are you sure you want to push " + selectedFileType + " " + selectedFile.getFileName() + " to Adobe Campaign?", (Stage) getNode().getScene().getWindow()) == YesNoPopupDialog.YesNo.NO) {
            return;
        }

        try {
            switch(selectedFileType) {
                case BLOCK:
                    campaignServerManager.updatePersonalizationBlock((PersonalizationBlockKey) selectedFile.getKey(), selectedFile.getWorkspaceFileContent());
                    break;
                case MODULE:
                    campaignServerManager.updateJavascriptTemplate((JavaScriptTemplateKey) selectedFile.getKey(), selectedFile.getWorkspaceFileContent());
                    break;
                default:
                    errorReporter.reportError("The file type " + selectedFileType + " is not supported for updating on Campaign!", true);
                    return;
            }
            errorReporter.logMessage("Updated " + selectedFileType + " " + selectedFile.getBaseFileName() + " on Adobe Campaign");
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred updating " + selectedFile.getBaseFileName() + " on Adobe Campaign", apiException, true);
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
