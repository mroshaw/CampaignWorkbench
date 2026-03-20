package com.campaignworkbench.ide.dialogs;

import com.campaignworkbench.adobecampaignapi.CampaignInstance;
import com.campaignworkbench.ide.AppSettings;
import com.campaignworkbench.ide.AppSettingsManager;
import com.campaignworkbench.ide.logging.ErrorReporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Settings dialog for managing Campaign instances.
 * Lists all configured instances and allows the user to add, edit, and remove them.
 * Credentials are never shown or stored outside the system Keyring.
 */
public class SettingsDialog {

    private static final String TITLE = "Settings";

    public static void show(Window owner, AppSettings appSettings, ErrorReporter errorReporter) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(TITLE);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        TabPane settingsTabPane = new TabPane();
        Tab instanceSettingsTab = new Tab("Instances");
        Tab viewSettingsTab = new Tab("View");

        settingsTabPane.getTabs().addAll(instanceSettingsTab, viewSettingsTab);

        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        // Instance list (working copy so changes are only committed on save within sub-dialogs)
        ObservableList<CampaignInstance> instanceItems =
                FXCollections.observableArrayList(appSettings.getInstances());

        ListView<CampaignInstance> instanceListView = new ListView<>(instanceItems);
        instanceListView.setPrefHeight(200);
        instanceListView.setPrefWidth(260);
        instanceListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(CampaignInstance item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Buttons
        Button addButton = new Button("Add...");
        Button editButton = new Button("Edit...");
        Button removeButton = new Button("Remove");

        editButton.setDisable(true);
        removeButton.setDisable(true);

        instanceListView.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
            editButton.setDisable(selected == null);
            removeButton.setDisable(selected == null);
        });

        addButton.setOnAction(_ -> {
            Optional<CampaignInstance> result = InstanceEditDialog.showForNew(owner, errorReporter);
            result.ifPresent(newInstance -> {
                appSettings.addInstance(newInstance);
                AppSettingsManager.save(appSettings);
                instanceItems.setAll(appSettings.getInstances());
            });
        });

        editButton.setOnAction(_ -> {
            CampaignInstance selected = instanceListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            InstanceEditDialog.showForEdit(owner, selected, errorReporter);
            // Credentials saved within the dialog; name change reflected via observable list refresh
            AppSettingsManager.save(appSettings);
            instanceItems.setAll(appSettings.getInstances());
        });

        removeButton.setOnAction(_ -> {
            CampaignInstance selected = instanceListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            boolean confirmed = YesNoPopupDialog.show(owner,
                    "Remove instance?",
                    "Remove '" + selected.getName() + "'? Stored credentials will also be deleted.") == YesNoPopupDialog.YesNo.YES;
            if (confirmed) {

                Optional<char[]> credentialPassword = PasswordInputDialog.show(owner, "Credentials Password", "Enter credentials password", "Please enter the password used to protect the instance credentials");

                if(credentialPassword.isPresent()) {
                    try {
                        selected.getCredentialStore().unlock(credentialPassword.get());
                    } catch (Exception ex) {
                        errorReporter.reportError("An error occurred while retrieving credentials!", ex, true);
                    }
                    selected.getCredentialStore().clear();
                    appSettings.removeInstance(selected);
                    AppSettingsManager.save(appSettings);
                    instanceItems.setAll(appSettings.getInstances());
                }

            }
        });

        VBox buttonBox = new VBox(8, addButton, editButton, removeButton);
        buttonBox.setAlignment(Pos.TOP_LEFT);

        HBox instancePanel = new HBox(12, instanceListView, buttonBox);
        instancePanel.setAlignment(Pos.TOP_LEFT);

        VBox instanceContent = new VBox(12);
        instanceContent.setPadding(new Insets(20));
        instanceContent.getChildren().addAll(new Label("Campaign Instances:"), instancePanel);

        instanceSettingsTab.setContent(instanceContent);

        dialog.getDialogPane().setContent(settingsTabPane);
        dialog.getDialogPane().setPrefWidth(420);

        dialog.showAndWait();
    }
}