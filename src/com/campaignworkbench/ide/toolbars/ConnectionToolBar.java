package com.campaignworkbench.ide.toolbars;

import com.campaignworkbench.adobecampaignapi.ConnectedStatus;
import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;

import java.util.Objects;

public class ConnectionToolBar implements IJavaFxNode {
    private final ToolBar toolBar;
    private final Button connectToCampaignButton;
    private final Button disconnectFromCampaignButton;
    private final Label connectedHostLabel;
    private SimpleBooleanProperty workspaceSetObservable;

    public ConnectionToolBar(Runnable connectHandler, Runnable disconnectHandler,
                             ObjectProperty<ConnectedStatus> connectedObservable, SimpleBooleanProperty workspaceSetObservable) {

        this.workspaceSetObservable = workspaceSetObservable;
        toolBar = new ToolBar();
        connectToCampaignButton = UiUtil.createToolbarButton("", "Connect to Campaign", IdeIcon.CONNECT, true, "positive-icon", 20, false, _ -> connectHandler.run());
        disconnectFromCampaignButton = UiUtil.createToolbarButton("", "Disconnect from Campaign", IdeIcon.DISCONNECT, true, "negative-icon", 20, false, _ -> disconnectHandler.run());
        connectedHostLabel = new Label(connectedObservable.getValue().getConnectionName());
        connectedHostLabel.getStyleClass().add("host-label");
        toolBar.getItems().addAll(connectToCampaignButton, disconnectFromCampaignButton, connectedHostLabel);

        workspaceSetObservable.addListener((_, _, newValue) -> connectToCampaignButton.setDisable(!newValue));
        connectedObservable.addListener((_, _, newValue) -> connectedStatusChangedHandler(newValue));

        connectedStatusChangedHandler(connectedObservable.getValue());
    }

    private void connectedStatusChangedHandler(ConnectedStatus connectedStatus) {
        connectedHostLabel.setText(Objects.equals(connectedStatus.getConnectionName(), "") ? "Not connected" : connectedStatus.getConnectionName() + " (" + connectedStatus.getConnectionHost() + ")");
        connectToCampaignButton.setDisable(connectedStatus.getIsConnected() || !workspaceSetObservable.getValue());
        disconnectFromCampaignButton.setDisable(!connectedStatus.getIsConnected());
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
