package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplateCollection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public class CampaignModulePickerDialog {

    /**
     * Shows a dialog allowing the user to pick an JavaScriptTemplate item.
     *
     * @param owner The owner window
     * @param title Dialog title
     * @param headerText Dialog header
     * @param contentText Dialog content text (optional)
     * @return Optional containing the selected IncludeView if chosen
     */
    public static Optional<JavaScriptTemplate> show(
            Window owner,
            String title,
            String headerText,
            String contentText
    ) {
        JavaScriptTemplateCollection allJavaScriptTemplates = CampaignServerManager.getAllJavaScriptTemplates(true);
        ObservableList<JavaScriptTemplate> javascriptTemplates = FXCollections.observableArrayList(allJavaScriptTemplates.getJavaScriptTemplates());

        Dialog<JavaScriptTemplate> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);

        // adjust the Stage once shown
        dialog.setOnShown(event -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();

            stage.setWidth(1000);
            stage.setHeight(700);

            // Center relative to owner
            if (owner != null) {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
            }
        });
        ButtonType pickButtonType = new ButtonType("Pick", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(pickButtonType, ButtonType.CANCEL);

        ListView<JavaScriptTemplate> listView = new ListView<>(javascriptTemplates);
        listView.getStyleClass().add("picker-listview");
        listView.setFixedCellSize(24);

        // Display each JavaScriptTemplate as "Label (name)"
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JavaScriptTemplate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : String.format("%s:%s", item.getNamespace(), item.getName()));
            }
        });

        if (!javascriptTemplates.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        }

        Node pickButton = dialog.getDialogPane().lookupButton(pickButtonType);
        pickButton.setDisable(listView.getSelectionModel().getSelectedItem() == null);

        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> pickButton.setDisable(newVal == null)
        );

        // Double-click selects
        listView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                JavaScriptTemplate selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    dialog.setResult(selected);
                    dialog.close();
                }
            }
        });

        dialog.getDialogPane().setContent(new BorderPane(listView));

        dialog.setResultConverter(button -> {
            if (button == pickButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        return dialog.showAndWait();
    }
}