package com.campaignworkbench.ide.dialogs;


import com.campaignworkbench.adobecampaignapi.CampaignInstance;
import com.campaignworkbench.ide.AppSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Dialog for creating a new workspace. Collects a workspace name and
 * the CampaignInstance to associate with it.
 */
public class NewWorkspaceDialog {

    public record Result(String workspaceName, CampaignInstance instance) {}

    public static Optional<Result> show(Window owner, AppSettings appSettings) {

        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Create Workspace");
        dialog.initOwner(owner);
        dialog.setResizable(false);

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        nameField.setPromptText("My Workspace");
        nameField.setPrefColumnCount(28);

        ComboBox<CampaignInstance> instanceCombo = new ComboBox<>();
        instanceCombo.getItems().addAll(appSettings.getInstances());
        instanceCombo.setPromptText("Select a Campaign instance...");
        instanceCombo.setPrefWidth(220);

        grid.add(new Label("Workspace name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Campaign instance:"), 0, 1);
        grid.add(instanceCombo, 1, 1);

        // Disable Create until both fields are filled
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        Runnable validateInputs = () -> {
            boolean nameEmpty = nameField.getText() == null || nameField.getText().trim().isEmpty();
            boolean noInstance = instanceCombo.getValue() == null;
            createButton.setDisable(nameEmpty || noInstance);
        };

        nameField.textProperty().addListener((_, _, _) -> validateInputs.run());
        instanceCombo.valueProperty().addListener((_, _, _) -> validateInputs.run());

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                return new Result(nameField.getText().trim(), instanceCombo.getValue());
            }
            return null;
        });

        return dialog.showAndWait();
    }
}