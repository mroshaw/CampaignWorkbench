package com.campaignworkbench.ide.dialogs;

import com.campaignworkbench.adobecampaignapi.CredentialStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

public class SettingsDialog {

    private static final String TITLE = "Campaign Configuration";

    public static void show(Window owner) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(TITLE);
        dialog.initOwner(owner);
        dialog.setResizable(false);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER_LEFT);

        // Fields
        TextField endpointField = new TextField();
        endpointField.setPromptText("https://your-server.example.com");
        endpointField.setPrefColumnCount(30);

        TextField instanceNameField = new TextField();
        instanceNameField.setPromptText("Development");
        instanceNameField.setPrefColumnCount(30);

        CredentialStore credentialStore = new CredentialStore();

        // Pre-populate URL only
        Optional<String> endpointUrl = credentialStore.getEndpointUrl();
        endpointUrl.ifPresent(endpointField::setText);
        PasswordField clientIdField = new PasswordField();
        clientIdField.setPromptText("Client ID");

        PasswordField clientSecretField = new PasswordField();
        clientSecretField.setPromptText("Client Secret");

        // Labels
        grid.add(new Label("Instance Name:"), 0, 0);
        grid.add(instanceNameField, 1, 0);

        grid.add(new Label("Connection URL:"), 0, 1);
        grid.add(endpointField, 1, 1);

        grid.add(new Label("Client ID:"), 0, 2);
        grid.add(clientIdField, 1, 2);

        grid.add(new Label("Client Secret:"), 0, 3);
        grid.add(clientSecretField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Save action
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {

                String instanceName = instanceNameField.getText() != null
                ? instanceNameField.getText().trim() : "";

                String newEndpointUrl = endpointField.getText() != null
                        ? endpointField.getText().trim()
                        : "";

                String newClientId = clientIdField.getText() != null
                        ? clientIdField.getText().trim()
                        : "";

                String newClientSecret = clientSecretField.getText() != null
                        ? clientSecretField.getText().trim()
                        : "";

                credentialStore.save(instanceName, newClientId, newClientSecret, newEndpointUrl);
            }
            return null;
        });

        dialog.showAndWait();
    }
}