package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.EtmModuleRecord;
import com.campaignworkbench.ide.dialogs.PickerDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public class CampaignModulePickerDialog extends PickerDialog<EtmModuleRecord> {

    public static Optional<EtmModuleRecord> show(Window owner, CampaignServerManager campaignServerManager) {
        return new CampaignModulePickerDialog().showInternal(owner, campaignServerManager);
    }

    public Optional<EtmModuleRecord> showInternal(Window owner, CampaignServerManager campaignServerManager) {

        ObservableList<EtmModuleRecord> modules = FXCollections.observableArrayList(
                campaignServerManager.getAllJavaScriptTemplates(true).getJavaScriptTemplates()
        );

        // Filter
        FilteredList<EtmModuleRecord> filteredModules = new FilteredList<>(modules, _ -> true);

        // Sort
        SortedList<EtmModuleRecord> sortedModules = new SortedList<>(filteredModules);

        // Table
        TableView<EtmModuleRecord> tableView = new TableView<>(sortedModules);
        tableView.getStyleClass().add("picker-tableview");
        sortedModules.comparatorProperty().bind(tableView.comparatorProperty());

        TableColumn<EtmModuleRecord, String> namespaceColumn = new TableColumn<>("Namespace");
        namespaceColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getNamespace())
        );
        namespaceColumn.setPrefWidth(300);

        TableColumn<EtmModuleRecord, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getName())
        );
        nameColumn.setPrefWidth(500);

        tableView.getColumns().addAll(namespaceColumn, nameColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Filter bar
        TextField filterField = new TextField();
        filterField.setPromptText("Filter...");
        HBox.setHgrow(filterField, Priority.ALWAYS);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(_ -> filterField.clear());

        filterField.textProperty().addListener((_, _, newValue) -> {
            String lower = newValue == null ? "" : newValue.toLowerCase();
            filteredModules.setPredicate(record -> {
                if (lower.isEmpty()) return true;
                String namespace = record.getNamespace() != null ? record.getNamespace().toLowerCase() : "";
                String name = record.getName() != null ? record.getName().toLowerCase() : "";
                return namespace.contains(lower) || name.contains(lower);
            });
        });

        HBox filterBar = new HBox(8, new Label("Filter:"), filterField, clearButton);
        filterBar.setPadding(new Insets(0, 0, 8, 0));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // Layout
        BorderPane content = new BorderPane();
        content.setTop(filterBar);
        content.setCenter(tableView);

        // Dialog
        Dialog<EtmModuleRecord> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Pick JavaScript Module");
        dialog.setHeaderText("Select a module");

        dialog.setOnShown(_ -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setWidth(1000);
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