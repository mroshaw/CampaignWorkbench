package com.campaignworkbench.ide.dialogs;

import com.campaignworkbench.adobecampaignapi.ApiException;
import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.FolderRecord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * Dialog to capture details needed to create a new personalization block on the Campaign server.
 */
public class CreateBlockOnServerDialog {

    public record Result(String name, String label, long folderId) {}

    public static Optional<Result> show(Window owner, String currentFileName, CampaignServerManager campaignServerManager) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Create Block on Server");
        dialog.setHeaderText("Enter details for the new personalization block");
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
        nameField.setPromptText("e.g. myBlock");
        nameField.setText(currentFileName);
        nameField.setPrefColumnCount(28);

        TextField labelField = new TextField();
        labelField.setPromptText("e.g. My Block");
        labelField.setPrefColumnCount(28);

        ComboBox<FolderRecord> folderComboBox = new ComboBox<>();
        folderComboBox.getStyleClass().add("folder-dropdown");
        try {
            List<FolderRecord> folders = campaignServerManager.getAllBlockFolders(true).getBlockFolders();
            folderComboBox.getItems().addAll(folders);
        } catch (Exception e) {
            throw new ApiException("An error occurred while trying to get the list of block folders.", e);
        }
        folderComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(FolderRecord item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName());
            }
        });

        folderComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(FolderRecord item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName());
            }
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Label:"), 0, 1);
        grid.add(labelField, 1, 1);
        grid.add(new Label("Folder:"), 0, 2);
        grid.add(folderComboBox, 1, 2);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        Runnable validate = () -> {
            boolean nameEmpty = nameField.getText() == null || nameField.getText().trim().isEmpty();
            boolean labelEmpty = labelField.getText() == null || labelField.getText().trim().isEmpty();
            boolean noFolder = folderComboBox.getValue() == null;
            createButton.setDisable(nameEmpty || labelEmpty || noFolder);
        };

        nameField.textProperty().addListener((_, _, _) -> validate.run());
        labelField.textProperty().addListener((_, _, _) -> validate.run());
        folderComboBox.valueProperty().addListener((_, _, _) -> validate.run());

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                return new Result(
                        nameField.getText().trim(),
                        labelField.getText().trim(),
                        folderComboBox.getValue().getId()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static boolean isValidLong(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        try {
            Long.parseLong(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}