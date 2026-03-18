package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.WorkspaceFile;
import com.campaignworkbench.workspace.WorkspaceFileType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CampaignToolBar implements IJavaFxNode {

    private final ToolBar toolBar;
    private final Button refreshFromCampaignButton;
    private final Button pushToCampaignButton;
    private final Button createOnCampaignButton;

    private final Supplier<WorkspaceFile> workspaceFileSupplier;

    public CampaignToolBar(Supplier<WorkspaceFile> workspaceFileSupplier,
                           SimpleBooleanProperty connectedObservable,
                           Consumer<WorkspaceFile> refreshConsumer,
                           Consumer<WorkspaceFile> pushConsumer,
                           Consumer<WorkspaceFile> createOnServerConsumer) {
        this.workspaceFileSupplier = workspaceFileSupplier;

        toolBar = new ToolBar();
        toolBar.getStyleClass().add("small-toolbar");
        refreshFromCampaignButton = UiUtil.createMiniToolbarButton("", "Refresh from Campaign", IdeIcon.REFRESH_FROM_CAMPAIGN, true, "neutral-icon", 20, false, _ -> refreshConsumer.accept(workspaceFileSupplier.get()));
        pushToCampaignButton = UiUtil.createMiniToolbarButton("", "Push to Campaign", IdeIcon.UPDATE_TO_CAMPAIGN, true, "neutral-icon", 20, false, _ -> pushConsumer.accept(workspaceFileSupplier.get()));
        createOnCampaignButton = UiUtil.createMiniToolbarButton("", "Create on Campaign", IdeIcon.CREATE_ON_CAMPAIGN, true, "neutral-icon", 20, false, _ -> createOnServerConsumer.accept(workspaceFileSupplier.get()));
        connectedObservable.addListener((_, _, newVal) -> updateButtonState(newVal));
        toolBar.getItems().addAll(refreshFromCampaignButton, pushToCampaignButton, createOnCampaignButton);

        // Set initial state directly from the property
        updateButtonState(connectedObservable.get());
    }

    private void updateButtonState(boolean connected) {
        WorkspaceFile workspaceFile = workspaceFileSupplier.get();
        boolean hasCampaignKey = workspaceFile != null && workspaceFile.hasCampaignKey();
        boolean isServerSyncable = workspaceFile != null &&
                (workspaceFile.getFileType() == WorkspaceFileType.BLOCK ||
                        workspaceFile.getFileType() == WorkspaceFileType.MODULE);
        refreshFromCampaignButton.setDisable(!connected || !hasCampaignKey);
        pushToCampaignButton.setDisable(!connected || !hasCampaignKey);
        createOnCampaignButton.setDisable(!connected || hasCampaignKey || !isServerSyncable);
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}