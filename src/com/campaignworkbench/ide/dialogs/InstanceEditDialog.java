package com.campaignworkbench.ide.dialogs;

import com.campaignworkbench.adobecampaignapi.CampaignInstance;
import com.campaignworkbench.adobecampaignapi.KeystorePasswordProvider;
import com.campaignworkbench.ide.logging.ErrorReporter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
    public static Optional<CampaignInstance> showForNew(Window owner, ErrorReporter errorReporter) {
        return show(owner, null, errorReporter);
    }

    /**
     * Shows the dialog to edit an existing CampaignInstance in place.
     * Saves credential changes directly to the Keyring on confirm.
     */
    public static void showForEdit(Window owner, CampaignInstance existing, ErrorReporter errorReporter) {
        show(owner, existing, errorReporter);
    }

    private static Optional<CampaignInstance> show(Window owner, CampaignInstance existing, ErrorReporter errorReporter) {
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
            endpointField.setText(existing.getEndpointUrl());
        }

        PasswordField clientIdField = new PasswordField();
        clientIdField.setPromptText(isNew ? "Client ID" : "Leave blank to keep existing");

        PasswordField clientSecretField = new PasswordField();
        clientSecretField.setPromptText(isNew ? "Client Secret" : "Leave blank to keep existing");

        PasswordField credentialPasswordField = new PasswordField();
        credentialPasswordField.setPromptText(isNew ? "Credentials Password" : "Re-enter password to save changes");

        CheckBox requireCredentialsPassword =  new CheckBox("Secure credentials with a password (recommended)");
        requireCredentialsPassword.setSelected(true);
        requireCredentialsPassword.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                credentialPasswordField.setDisable(!newValue);
                credentialPasswordField.setText("");
            }
        });
        requireCredentialsPassword.setDisable(!isNew);

        grid.add(new Label("Instance name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Connection URL:"), 0, 1);
        grid.add(endpointField, 1, 1);
        grid.add(new Label("Client ID:"), 0, 2);
        grid.add(clientIdField, 1, 2);
        grid.add(new Label("Client Secret:"), 0, 3);
        grid.add(clientSecretField, 1, 3);
        grid.add(new Label("Require Password:"), 0, 4);
        grid.add(requireCredentialsPassword, 1, 4);
        grid.add(new Label("Password:"), 0, 5);
        grid.add(credentialPasswordField, 1, 5);

        // Disable Save until name and endpoint are filled
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable validateInputs = () -> {
            boolean nameEmpty = nameField.getText() == null || nameField.getText().trim().isEmpty();
            boolean urlEmpty = endpointField.getText() == null || endpointField.getText().trim().isEmpty();
            boolean clientIdEmpty = (clientIdField.getText() == null || clientIdField.getText().trim().isEmpty()) && isNew;
            boolean clientSecretEmpty = (clientSecretField.getText() == null || clientSecretField.getText().trim().isEmpty()) && isNew;
            boolean passwordEmpty = credentialPasswordField.getText() == null || credentialPasswordField.getText().trim().isEmpty();
            boolean passwordRequired = requireCredentialsPassword.isSelected();
            saveButton.setDisable(nameEmpty || urlEmpty || clientIdEmpty || clientSecretEmpty || (passwordEmpty && passwordRequired));
        };

        nameField.textProperty().addListener((_, _, _) -> validateInputs.run());
        endpointField.textProperty().addListener((_, _, _) -> validateInputs.run());
        clientIdField.textProperty().addListener((_, _, _) -> validateInputs.run());
        clientSecretField.textProperty().addListener((_, _, _) -> validateInputs.run());
        credentialPasswordField.textProperty().addListener((_, _, _) -> validateInputs.run());
        requireCredentialsPassword.selectedProperty().addListener((_, _, _) -> validateInputs.run());

        // Run once to set initial state (edit mode may already have valid values)
        validateInputs.run();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) return null;

            String name = nameField.getText().trim();
            String endpointUrl = endpointField.getText().trim();
            String clientId = clientIdField.getText();
            String clientSecret = clientSecretField.getText();
            char[] credentialPassword = requireCredentialsPassword.isSelected() ? credentialPasswordField.getText().toCharArray() : KeystorePasswordProvider.getPassword();

            if (isNew) {
                CampaignInstance instance = new CampaignInstance(name, endpointUrl);
                try {
                    instance.getCredentialStore().unlock(credentialPassword);
                    instance.getCredentialStore().save(clientId, clientSecret);
                } catch (Exception ex) {
                    errorReporter.reportError("An error occurred while retrieving credentials!", ex, true);
                }
                return instance;
            } else {
                existing.setName(name);
                // For edit: only update credentials that were provided
                try {
                    existing.getCredentialStore().unlock(credentialPassword);
                } catch (Exception ex) {
                    errorReporter.reportError("An error occurred while updating credentials!", ex, true);
                    return existing;
                }
                String existingClientId = existing.getCredentialStore().getClientId().orElse("");
                String existingClientSecret = existing.getCredentialStore().getClientSecret().orElse("");
                existing.getCredentialStore().save(
                        clientId.isEmpty() ? existingClientId : clientId,
                        clientSecret.isEmpty() ? existingClientSecret : clientSecret);
                return existing;
            }
        });

        return dialog.showAndWait();
    }
}