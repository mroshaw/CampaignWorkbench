package com.campaignworkbench.ide.dialogs;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.PersoBlockRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public class CampaignBlockPickerDialog extends PickerDialog<PersoBlockRecord> {

    public static Optional<PersoBlockRecord> show(Window owner, CampaignServerManager campaignServerManager) {
        return new CampaignBlockPickerDialog().showInternal(owner, campaignServerManager);
    }

    public Optional<PersoBlockRecord> showInternal(Window owner, CampaignServerManager campaignServerManager) {

        ObservableList<PersoBlockRecord> blocks = FXCollections.observableArrayList(
                campaignServerManager.getAllPersoBlocks(true).getPersonalisationBlocks()
        );

        // Filter
        FilteredList<PersoBlockRecord> filteredBlocks = new FilteredList<>(blocks, _ -> true);

        // Sort
        SortedList<PersoBlockRecord> sortedBlocks = new SortedList<>(filteredBlocks);

        // Table
        TableView<PersoBlockRecord> tableView = new TableView<>(sortedBlocks);
        tableView.getStyleClass().add("picker-tableview");
        sortedBlocks.comparatorProperty().bind(tableView.comparatorProperty());

        TableColumn<PersoBlockRecord, String> folderColumn = new TableColumn<>("Folder");
        folderColumn.setCellValueFactory(cell -> {
            PersoBlockRecord record = cell.getValue();
            String folder = record.getFolder() != null ? record.getFolder().getFullName() : "";
            return new javafx.beans.property.SimpleStringProperty(folder);
        });
        folderColumn.setPrefWidth(600);

        TableColumn<PersoBlockRecord, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getLabel())
        );
        nameColumn.setPrefWidth(400);

        tableView.getColumns().addAll(folderColumn, nameColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Filter bar
        TextField filterField = new TextField();
        filterField.setPromptText("Filter...");
        HBox.setHgrow(filterField, javafx.scene.layout.Priority.ALWAYS);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(_ -> filterField.clear());

        filterField.textProperty().addListener((_, _, newValue) -> {
            String lower = newValue == null ? "" : newValue.toLowerCase();
            filteredBlocks.setPredicate(record -> {
                if (lower.isEmpty()) return true;
                String folder = record.getFolder() != null ? record.getFolder().getFullName().toLowerCase() : "";
                String label = record.getLabel() != null ? record.getLabel().toLowerCase() : "";
                return folder.contains(lower) || label.contains(lower);
            });
        });

        HBox filterBar = new HBox(8, new Label("Filter:"), filterField, clearButton);
        filterBar.setPadding(new Insets(0, 0, 8, 0));
        filterBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Layout
        BorderPane content = new BorderPane();
        content.setTop(filterBar);
        content.setCenter(tableView);

        // Dialog
        Dialog<PersoBlockRecord> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Pick Personalization Block");
        dialog.setHeaderText("Select a block");

        dialog.setOnShown(_ -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setWidth(1200);
            stage.setHeight(700);
            if (owner != null) {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
            }
            filterField.requestFocus();
        });

        ButtonType pickButtonType = new ButtonType("Pick", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(pickButtonType, ButtonType.CANCEL);

        Node pickButton = dialog.getDialogPane().lookupButton(pickButtonType);
        pickButton.setDisable(true);

        tableView.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) ->
                pickButton.setDisable(newVal == null)
        );

        tableView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(tableView.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button ->
                button == pickButtonType ? tableView.getSelectionModel().getSelectedItem() : null
        );

        return dialog.showAndWait();
    }
}