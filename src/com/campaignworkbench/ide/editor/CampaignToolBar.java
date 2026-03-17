package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.WorkspaceFile;
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

    private Boolean isCampaignConnected = false;

    Consumer<WorkspaceFile> refreshConsumer;
    Consumer<WorkspaceFile> pushConsumer;

    private final EditorTab editorTab;

    public CampaignToolBar(EditorTab editorTab, SimpleBooleanProperty connectedObservable, Supplier<Boolean> connectedStateSupplier, Consumer<WorkspaceFile> refreshConsumer, Consumer<WorkspaceFile> pushConsumer) {
        this.editorTab = editorTab;
        this.refreshConsumer = refreshConsumer;
        this.pushConsumer = pushConsumer;

        toolBar = new ToolBar();
        toolBar.getStyleClass().add("small-toolbar");
        refreshFromCampaignButton = UiUtil.createMiniToolbarButton("", "Refresh from Campaign", IdeIcon.REFRESH_FROM_CAMPAIGN, true, "neutral-icon", 20, false, _ -> refreshHandler());
        pushToCampaignButton = UiUtil.createMiniToolbarButton("", "Push to Campaign", IdeIcon.UPDATE_TO_CAMPAIGN, true, "neutral-icon", 20, false, _ -> pushHandler());
        connectedObservable.addListener((obs, oldVal, newVal) -> connectedChangedHandler(newVal));
        toolBar.getItems().addAll(refreshFromCampaignButton, pushToCampaignButton);
        isCampaignConnected = connectedStateSupplier.get();
        setButtonState();
    }

    private void setButtonState() {
        refreshFromCampaignButton.setDisable(!isCampaignConnected || !editorTab.getWorkspaceFile().hasCampaignKey());
        pushToCampaignButton.setDisable(!isCampaignConnected || !editorTab.getWorkspaceFile().hasCampaignKey());
    }

    private void connectedChangedHandler(Boolean connected) {
        isCampaignConnected = connected;
        setButtonState();
    }

    private void refreshHandler() {
        refreshConsumer.accept(editorTab.getWorkspaceFile());
    }

    private void pushHandler() {
        pushConsumer.accept(editorTab.getWorkspaceFile());
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
