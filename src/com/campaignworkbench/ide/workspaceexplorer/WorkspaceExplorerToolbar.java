package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.ConnectedStatus;
import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.Workspace;
import com.campaignworkbench.workspace.WorkspaceFile;
import com.campaignworkbench.workspace.WorkspaceFileType;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WorkspaceExplorerToolbar implements IJavaFxNode {

    // Context menu labels
    private static final String createNewButtonText = "Create new";
    private static final String addExistingButtonText = "Add existing";
    private static final String removeButtonText = "Remove";

    // Workspace toolbar buttons
    private final Button createNewButton;
    private final Button addExistingButton;
    private final Button removeButton;
    private final Button setDataContextButton;
    private final Button clearDataContextButton;
    private final Button setMessageContextButton;
    private final Button clearMessageContextButton;

    // Campaign toolbar buttons
    private final Button createNewFromCampaignButton;

    private final Label urlHostLabel;

    private final ToolBar toolBar;

    private final Consumer<WorkspaceFile> openFileConsumer;
    private final Supplier<Workspace> workspaceSupplier;
    private final Supplier<WorkspaceFile> selectedFileSupplier;
    private final Supplier<WorkspaceFileType> selectedFileTypeSupplier;
    private final ObjectProperty<ConnectedStatus> connectedObservable;

    private ErrorReporter errorReporter;

    public WorkspaceExplorerToolbar(
            Supplier<Workspace> workspaceSupplier,
            Supplier<WorkspaceFile> selectedFileSupplier,
            Supplier<WorkspaceFileType> selectedFileTypeSupplier,
            ObjectProperty<ConnectedStatus> connectedObservable,
            Consumer<WorkspaceFile> openFileConsumer,
            Runnable createNew,
            Runnable addExisting,
            Runnable remove,
            Runnable setDataContext,
            Runnable clearDataContext,
            Runnable setMessageContext,
            Runnable clearMessageContext,
            Runnable createFromServer,
            ErrorReporter errorReporter
    ) {

        this.workspaceSupplier = workspaceSupplier;
        this.selectedFileSupplier = selectedFileSupplier;
        this.selectedFileTypeSupplier = selectedFileTypeSupplier;
        this.connectedObservable = connectedObservable;
        this.openFileConsumer = openFileConsumer;
        this.errorReporter = errorReporter;

        createNewButton = UiUtil.createMiniToolbarButton("", "Create new", IdeIcon.NEW_FILE, true, "neutral-icon", 20, true, _ -> createNew.run());
        addExistingButton = UiUtil.createMiniToolbarButton("", "Add existing", IdeIcon.ADD_FILE, true, "positive-icon", 20, true, _ -> addExisting.run());
        removeButton = UiUtil.createMiniToolbarButton("", "Remove", IdeIcon.DELETE_FILE, true, "negative-icon", 20, true, _ -> remove.run());
        setDataContextButton = UiUtil.createMiniToolbarButton("", "Set Data Context", IdeIcon.SET_DATA_CONTEXT, true, "positive-icon", 20, true, _ -> setDataContext.run());
        clearDataContextButton = UiUtil.createMiniToolbarButton("", "Clear Data Context", IdeIcon.CLEAR_DATA_CONTEXT, true, "negative-icon", 20, true, _ -> clearDataContext.run());
        setMessageContextButton = UiUtil.createMiniToolbarButton("", "Set Message Context", IdeIcon.SET_MESSAGE_CONTEXT, true, "positive-icon", 20, true, _ -> setMessageContext.run());
        clearMessageContextButton = UiUtil.createMiniToolbarButton("", "Clear Message Context", IdeIcon.CLEAR_MESSAGE_CONTEXT, true, "negative-icon", 20, true, _ -> clearMessageContext.run());
        createNewFromCampaignButton = UiUtil.createMiniToolbarButton("", "Create new from Campaign", IdeIcon.NEW_FROM_CAMPAIGN, true, "positive-icon", 20, false, _ -> createFromServer.run());

        urlHostLabel = new Label();

        toolBar = new ToolBar(createNewButton, addExistingButton, createNewFromCampaignButton, removeButton, setDataContextButton, clearDataContextButton, setMessageContextButton, clearMessageContextButton);
        toolBar.getStyleClass().add("small-toolbar");

        connectedObservable.addListener((observable, oldValue, newValue) -> {
            update();
        });
    }

    public ToolBar getToolBar() {
        return toolBar;
    }

    public void update() {
        boolean isConnected =  connectedObservable.getValue().getIsConnected();
        WorkspaceFile selectedFile = selectedFileSupplier.get();
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();
        boolean hasWorkspace = workspaceSupplier.get() != null;
        boolean hasFileSelected = selectedFile != null;
        boolean hasFileTypeSelected = selectedFileType != null;
        boolean isTemplate = selectedFileType == WorkspaceFileType.TEMPLATE;
        boolean isModule = selectedFileType == WorkspaceFileType.MODULE;
        boolean isBlock = selectedFileType == WorkspaceFileType.BLOCK;
        boolean hasCampaignKey = hasFileSelected && selectedFile.hasCampaignKey();

        createNewButton.setDisable(!hasWorkspace || !hasFileTypeSelected);
        addExistingButton.setDisable(!hasWorkspace || !hasFileTypeSelected);
        removeButton.setDisable(!hasWorkspace || !hasFileSelected);

        createNewFromCampaignButton.setDisable(!hasWorkspace || !isConnected || (!isModule && !isBlock));

        setDataContextButton.setDisable(!hasFileSelected || isBlock || !isTemplate && !isModule);
        clearDataContextButton.setDisable(!hasFileSelected || isBlock || !isTemplate && !isModule);
        setMessageContextButton.setDisable(!hasFileSelected || !isTemplate);
        clearMessageContextButton.setDisable(!hasFileSelected || !isTemplate);

        if (hasFileTypeSelected) {
            createNewButton.setTooltip(new Tooltip(createNewButtonText + " " + selectedFileType.toString().toLowerCase()));
            addExistingButton.setTooltip(new Tooltip(addExistingButtonText + " " + selectedFileType.toString().toLowerCase()));
            removeButton.setTooltip(new Tooltip(removeButtonText + " " + selectedFileType.toString().toLowerCase()));
        }
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}