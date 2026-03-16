package com.campaignworkbench.ide.dialogs;

import com.campaignworkbench.adobecampaignapi.CampaignInstance;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Dialog for adding a new CampaignInstance or editing an existing one.
 * The endpoint URL is shown for reference on edit, but credentials (client ID,
 * client secret) are write-only — they are sent directly to the Keyring and
 * never displayed.
 */
public class InstanceEditDialog {

    /**
     * Shows the dialog to create a new CampaignInstance.
     * Returns the created instance if the user confirmed, empty otherwise.
     */
    public static Optional<CampaignInstance> showForNew(Window owner) {
        return show(owner, null);
    }

    /**
     * Shows the dialog to edit an existing CampaignInstance in place.
     * Saves credential changes directly to the Keyring on confirm.
     */
    public static void showForEdit(Window owner, CampaignInstance existing) {
        show(owner, existing);
    }

    private static Optional<CampaignInstance> show(Window owner, CampaignInstance existing) {
        boolean isNew = existing == null;

        Dialog<CampaignInstance> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Campaign Instance" : "Edit Campaign Instance");
        dialog.initOwner(owner);
        dialog.setResizable(false);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField(isNew ? "" : existing.getName());
        nameField.setPromptText("e.g. Development");
        nameField.setPrefColumnCount(28);

        TextField endpointField = new TextField();
        endpointField.setPromptText("https://your-server.example.com");
        endpointField.setPrefColumnCount(28);

        // Pre-populate endpoint URL for edit (it is non-sensitive)
        if (!isNew) {
            existing.getCredentialStore().getEndpointUrl().ifPresent(endpointField::setText);
        }

        PasswordField clientIdField = new PasswordField();
        clientIdField.setPromptText(isNew ? "Client ID" : "Leave blank to keep existing");

        PasswordField clientSecretField = new PasswordField();
        clientSecretField.setPromptText(isNew ? "Client Secret" : "Leave blank to keep existing");

        int row = 0;
        grid.add(new Label("Instance name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Connection URL:"), 0, row);
        grid.add(endpointField, 1, row++);
        grid.add(new Label("Client ID:"), 0, row);
        grid.add(clientIdField, 1, row++);
        grid.add(new Label("Client Secret:"), 0, row);
        grid.add(clientSecretField, 1, row);

        // Disable Save until name and endpoint are filled
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable validateInputs = () -> {
            boolean nameEmpty = nameField.getText() == null || nameField.getText().trim().isEmpty();
            boolean urlEmpty = endpointField.getText() == null || endpointField.getText().trim().isEmpty();
            saveButton.setDisable(nameEmpty || urlEmpty);
        };

        nameField.textProperty().addListener((_, _, _) -> validateInputs.run());
        endpointField.textProperty().addListener((_, _, _) -> validateInputs.run());

        // Run once to set initial state (edit mode may already have valid values)
        validateInputs.run();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) return null;

            String name = nameField.getText().trim();
            String endpointUrl = endpointField.getText().trim();
            String clientId = clientIdField.getText() != null ? clientIdField.getText().trim() : "";
            String clientSecret = clientSecretField.getText() != null ? clientSecretField.getText().trim() : "";

            if (isNew) {
                CampaignInstance instance = new CampaignInstance(name);
                instance.getCredentialStore().save(clientId, clientSecret, endpointUrl);
                return instance;
            } else {
                existing.setName(name);
                // For edit: only update credentials that were provided
                String existingClientId = existing.getCredentialStore().getClientId().orElse("");
                String existingClientSecret = existing.getCredentialStore().getClientSecret().orElse("");
                existing.getCredentialStore().save(
                        clientId.isEmpty() ? existingClientId : clientId,
                        clientSecret.isEmpty() ? existingClientSecret : clientSecret,
                        endpointUrl
                );
                return existing;
            }
        });

        return dialog.showAndWait();
    }
}