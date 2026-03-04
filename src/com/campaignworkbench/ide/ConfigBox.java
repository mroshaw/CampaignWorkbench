package com.campaignworkbench.ide;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public class ConfigBox {

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

        // Pre-populate URL only
        endpointField.setText(CampaignServerManager.getEndpointUrl());

        PasswordField clientIdField = new PasswordField();
        clientIdField.setPromptText("Client ID");

        PasswordField clientSecretField = new PasswordField();
        clientSecretField.setPromptText("Client Secret");

        // Labels
        grid.add(new Label("Connection URL:"), 0, 0);
        grid.add(endpointField, 1, 0);

        grid.add(new Label("Client ID:"), 0, 1);
        grid.add(clientIdField, 1, 1);

        grid.add(new Label("Client Secret:"), 0, 2);
        grid.add(clientSecretField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Save action
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {

                String endpointUrl = endpointField.getText() != null
                        ? endpointField.getText().trim()
                        : "";

                String clientId = clientIdField.getText() != null
                        ? clientIdField.getText().trim()
                        : "";

                String clientSecret = clientSecretField.getText() != null
                        ? clientSecretField.getText().trim()
                        : "";

                CampaignServerManager.updateCredentials(clientId, clientSecret, endpointUrl);
                LogPanel.appendLog("Configuration saved.");
            }
            return null;
        });

        dialog.showAndWait();
    }
}