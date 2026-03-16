package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.Workspace;
import com.campaignworkbench.workspace.WorkspaceFile;
import com.campaignworkbench.workspace.WorkspaceFileType;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WorkspaceExplorerToolbar {

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
    private final Button connectToCampaignButton;
    private final Button disconnectFromCampaignButton;
    private final Button createNewFromCampaignButton;
    private final Button refreshFromCampaignButton;
    private final Button pushToCampaignButton;

    private final Label urlHostLabel;

    private final ToolBar workspaceToolbar;
    private final ToolBar campaignToolbar;

    private final Consumer<WorkspaceFile> openFileConsumer;
    private final CampaignOperationsHandler campaignOperationsHandler;
    private final Supplier<Workspace> workspaceSupplier;
    private final Supplier<WorkspaceFile> selectedFileSupplier;
    private final Supplier<WorkspaceFileType> selectedFileTypeSupplier;

    public WorkspaceExplorerToolbar(
            CampaignOperationsHandler campaignOperationsHandler,
            Supplier<Workspace> workspaceSupplier,
            Supplier<WorkspaceFile> selectedFileSupplier,
            Supplier<WorkspaceFileType> selectedFileTypeSupplier,
            Consumer<WorkspaceFile> openFileConsumer,
            Runnable createNew,
            Runnable addExisting,
            Runnable remove,
            Runnable setDataContext,
            Runnable clearDataContext,
            Runnable setMessageContext,
            Runnable clearMessageContext
            ) {

        this.campaignOperationsHandler = campaignOperationsHandler;
        this.workspaceSupplier = workspaceSupplier;
        this.selectedFileSupplier = selectedFileSupplier;
        this.selectedFileTypeSupplier = selectedFileTypeSupplier;
        this.openFileConsumer = openFileConsumer;

        createNewButton = UiUtil.createMiniToolbarButton("", "Create new", IdeIcon.NEW_FILE, true, "neutral-icon", 20, true, _ -> createNew.run());
        addExistingButton = UiUtil.createMiniToolbarButton("", "Add existing", IdeIcon.ADD_FILE, true, "positive-icon", 20, true, _ -> addExisting.run());
        removeButton = UiUtil.createMiniToolbarButton("", "Remove", IdeIcon.DELETE_FILE, true, "negative-icon", 20, true, _ -> remove.run());
        setDataContextButton = UiUtil.createMiniToolbarButton("", "Set Data Context", IdeIcon.SET_DATA_CONTEXT, true, "positive-icon", 20, true, _ -> setDataContext.run());
        clearDataContextButton = UiUtil.createMiniToolbarButton("", "Clear Data Context", IdeIcon.CLEAR_DATA_CONTEXT, true, "negative-icon", 20, true, _ -> clearDataContext.run());
        setMessageContextButton = UiUtil.createMiniToolbarButton("", "Set Message Context", IdeIcon.SET_MESSAGE_CONTEXT, true, "positive-icon", 20, true, _ -> setMessageContext.run());
        clearMessageContextButton = UiUtil.createMiniToolbarButton("", "Clear Message Context", IdeIcon.CLEAR_MESSAGE_CONTEXT, true, "negative-icon", 20, true, _ -> clearMessageContext.run());

        connectToCampaignButton = UiUtil.createMiniToolbarButton("", "Connect to Campaign", IdeIcon.CONNECT, true, "positive-icon", 20, true, _ -> withBusyCursor(campaignOperationsHandler::connectToCampaignHandler));
        disconnectFromCampaignButton = UiUtil.createMiniToolbarButton("", "Disconnect from Campaign", IdeIcon.DISCONNECT, true, "negative-icon", 20, false, _ -> campaignOperationsHandler.disconnectFromCampaignHandler());
        createNewFromCampaignButton = UiUtil.createMiniToolbarButton("", "Create new from Campaign", IdeIcon.NEW_FROM_CAMPAIGN, true, "positive-icon", 20, false, _ -> {
            Optional<Object> selection = campaignOperationsHandler.confirmCreateNew();
            selection.ifPresent(record ->
                    withBusyCursor(() -> {
                        campaignOperationsHandler.executeCreateNew(record);
                        // Platform.runLater(() -> openFileConsumer.accept(selectedFileSupplier.get()));
                    })
            );
        });
        refreshFromCampaignButton = UiUtil.createMiniToolbarButton("", "Refresh from Campaign", IdeIcon.REFRESH_FROM_CAMPAIGN, true, "neutral-icon", 20, false, _ -> {
            if (campaignOperationsHandler.confirmRefresh()) {
                withBusyCursor(() -> {
                    campaignOperationsHandler.executeRefresh();
                    // openFileConsumer.accept(selectedFileSupplier.get());
                });
            }
        });
        pushToCampaignButton = UiUtil.createMiniToolbarButton("", "Push to Campaign", IdeIcon.UPDATE_TO_CAMPAIGN, true, "neutral-icon", 20, false, _ -> {
            Optional<Boolean> confirmation = campaignOperationsHandler.confirmPush();
            confirmation.ifPresent(doBackup ->
                    withBusyCursor(() -> campaignOperationsHandler.executePush(doBackup))
            );
        });
        urlHostLabel = new Label();

        workspaceToolbar = new ToolBar(createNewButton, addExistingButton, removeButton, setDataContextButton, clearDataContextButton, setMessageContextButton, clearMessageContextButton);
        campaignToolbar = new ToolBar(connectToCampaignButton, disconnectFromCampaignButton, createNewFromCampaignButton, refreshFromCampaignButton, pushToCampaignButton, urlHostLabel);

        workspaceToolbar.getStyleClass().add("small-toolbar");
        campaignToolbar.getStyleClass().add("small-toolbar");
    }

    public ToolBar getWorkspaceToolbar() {
        return workspaceToolbar;
    }

    public ToolBar getCampaignToolbar() {
        return campaignToolbar;
    }

    public void onCampaignConnectionStateChanged() {
        urlHostLabel.setText(campaignOperationsHandler.getConnectionHostLabel());
        update();
    }

    public void update() {
        WorkspaceFile selectedFile = selectedFileSupplier.get();
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();
        boolean isConnected = campaignOperationsHandler.isConnectedToCampaign();
        boolean hasWorkspace = workspaceSupplier.get() != null;
        boolean hasFileSelected = selectedFile != null;
        boolean hasFileTypeSelected = selectedFileType != null;
        boolean isTemplate = selectedFileType == WorkspaceFileType.TEMPLATE;
        boolean isModule = selectedFileType == WorkspaceFileType.MODULE;
        boolean isBlock = selectedFileType == WorkspaceFileType.BLOCK;
        boolean hasCampaignKey = hasFileSelected && selectedFile.hasCampaignKey();
        boolean canSyncWithCampaign = hasFileSelected && isConnected && hasCampaignKey;

        connectToCampaignButton.setDisable(!hasWorkspace || isConnected);
        disconnectFromCampaignButton.setDisable(!hasWorkspace || !isConnected);

        createNewButton.setDisable(!hasWorkspace || !hasFileTypeSelected);
        addExistingButton.setDisable(!hasWorkspace || !hasFileTypeSelected);
        removeButton.setDisable(!hasWorkspace || !hasFileSelected);

        createNewFromCampaignButton.setDisable(!hasWorkspace || !isConnected || (!isModule && !isBlock));
        pushToCampaignButton.setDisable(!hasWorkspace || !(isTemplate ? (hasFileSelected && isConnected) : canSyncWithCampaign));
        refreshFromCampaignButton.setDisable(!hasWorkspace || !canSyncWithCampaign || isTemplate);

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

    private void withBusyCursor(Runnable operation) {
        Scene scene = campaignToolbar.getScene();
        scene.setCursor(Cursor.WAIT);
        Thread.ofVirtual().start(() -> {
            try {
                operation.run();
            } finally {
                Platform.runLater(() -> scene.setCursor(Cursor.DEFAULT));
            }
        });
    }
}