package com.campaignworkbench.ide.dialogs;

import com.campaignworkbench.adobecampaignapi.ApiException;
import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.EntitySchemaRecord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * Dialog to capture details needed to create a new JavaScript template on the Campaign server.
 */
public class CreateModuleOnServerDialog {

    public record Result(String namespace, String name, String label, String schemaKey) {}

    public static Optional<Result> show(Window owner, String currentFileName, CampaignServerManager campaignServerManager) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Create Module on Server");
        dialog.setHeaderText("Enter details for the new JavaScript template");
        dialog.initOwner(owner);
        dialog.setResizable(false);

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER_LEFT);

        TextField namespaceField = new TextField();
        namespaceField.setPromptText("e.g. ssg");
        namespaceField.setPrefColumnCount(28);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. ETM_M00_MyModule.js");
        nameField.setPrefColumnCount(28);
        nameField.setText(currentFileName);

        TextField labelField = new TextField();
        labelField.setPromptText("e.g. My Module");
        labelField.setPrefColumnCount(28);

        ComboBox<EntitySchemaRecord> schemaComboBox = new ComboBox<>();
        schemaComboBox.getStyleClass().add("schema-dropdown");
        try {
            List<EntitySchemaRecord> schemas = campaignServerManager.getAllSchemas(true).getSchemas();
            schemaComboBox.getItems().addAll(schemas);
        } catch (Exception e) {
            throw new ApiException("An error occurred while trying to get the list of schemas.", e);
        }
        schemaComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(EntitySchemaRecord item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNamespace() + ":" +  item.getName());
            }
        });

        schemaComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EntitySchemaRecord item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNamespace() + ":" +  item.getName());
            }
        });

        grid.add(new Label("Namespace:"), 0, 0);
        grid.add(namespaceField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Label:"), 0, 2);
        grid.add(labelField, 1, 2);
        grid.add(new Label("Schema:"), 0, 3);
        grid.add(schemaComboBox, 1, 3);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        Runnable validate = () -> {
            boolean namespaceEmpty = namespaceField.getText() == null || namespaceField.getText().trim().isEmpty();
            boolean nameEmpty = nameField.getText() == null || nameField.getText().trim().isEmpty();
            boolean labelEmpty = labelField.getText() == null || labelField.getText().trim().isEmpty();
            boolean  schemaEmpty = schemaComboBox.getSelectionModel().getSelectedItem() == null;
            createButton.setDisable(schemaEmpty || namespaceEmpty || nameEmpty || labelEmpty);
        };

        namespaceField.textProperty().addListener((_, _, _) -> validate.run());
        nameField.textProperty().addListener((_, _, _) -> validate.run());
        labelField.textProperty().addListener((_, _, _) -> validate.run());
        schemaComboBox.valueProperty().addListener((_, _, _) -> validate.run());

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                return new Result(
                        namespaceField.getText().trim(),
                        nameField.getText().trim(),
                        labelField.getText().trim(),
                        schemaComboBox.getValue().getNamespace() + ":" +  schemaComboBox.getValue().getName()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }
}