package com.campaignworkbench.ide.toolbars;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;

import java.util.function.Supplier;

public class CampaignConnectionToolBar implements IJavaFxNode {
    private final ToolBar toolBar;
    private final Button connectToCampaignButton;
    private final Button disconnectFromCampaignButton;
    private final Label connectedHostLabel;
    private final Supplier<String> connectSupplier;
    private final Supplier<Boolean> disconnectSupplier;

    public CampaignConnectionToolBar(Supplier<String> connectSupplier, Supplier<Boolean> disconnectSupplier,
                                     SimpleBooleanProperty connectedObservable, SimpleBooleanProperty workspaceSetObservable) {
        this.connectSupplier = connectSupplier;
        this.disconnectSupplier = disconnectSupplier;

        toolBar = new ToolBar();
        connectToCampaignButton = UiUtil.createToolbarButton("", "Connect to Campaign", IdeIcon.CONNECT, true, "positive-icon", 20, false, _ -> connectHandler());
        disconnectFromCampaignButton = UiUtil.createToolbarButton("", "Disconnect from Campaign", IdeIcon.DISCONNECT, true, "negative-icon", 20, false, _ -> disconnectHandler());
        connectedHostLabel = new Label("NOT CONNECTED");
        connectedHostLabel.getStyleClass().add("host-label");
        toolBar.getItems().addAll(connectToCampaignButton, disconnectFromCampaignButton, connectedHostLabel);

        workspaceSetObservable.addListener((observable, oldValue, newValue) -> {
            connectToCampaignButton.setDisable(!newValue);
        });

        connectedObservable.addListener((observable, _, newValue) -> {
            connectToCampaignButton.setDisable(newValue);
            disconnectFromCampaignButton.setDisable(!newValue);
            if(!newValue) {
                Platform.runLater(() -> connectedHostLabel.setText("DISCONNECTED FROM HOST"));
            }
        });
    }

    private void connectHandler() {
        String connectedHost = connectSupplier.get();
        connectedHostLabel.setText(connectedHost);
    }

    private void disconnectHandler() {
        if(disconnectSupplier.get()) {
            connectedHostLabel.setText("DISCONNECTED FROM HOST");
        }
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
