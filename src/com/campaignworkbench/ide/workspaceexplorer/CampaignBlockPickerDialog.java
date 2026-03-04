package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlock;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlockCollection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Comparator;
import java.util.Optional;

public class CampaignBlockPickerDialog {

    /**
     * Shows a dialog allowing the user to pick an IncludeView block.
     *
     * @param owner The owner window
     * @param title Dialog title
     * @param headerText Dialog header
     * @param contentText Dialog content text (optional)
     * @return Optional containing the selected IncludeView if chosen
     */
    public static Optional<PersonalizationBlock> show(
            Window owner,
            String title,
            String headerText,
            String contentText
    ) {
        PersonalizationBlockCollection allBlocks = CampaignServerManager.getAllPersoBlocks(true);
        ObservableList<PersonalizationBlock> blocks = FXCollections.observableArrayList(allBlocks.getPersonalisationBlocks());

        Dialog<PersonalizationBlock> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        Window window = dialog.getDialogPane().getScene().getWindow();

        // adjust the Stage once shown
        dialog.setOnShown(event -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();

            stage.setWidth(1200);
            stage.setHeight(700);

            // Center relative to owner
            if (owner != null) {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
            }
        });

        ButtonType pickButtonType = new ButtonType("Pick", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(pickButtonType, ButtonType.CANCEL);

        ListView<PersonalizationBlock> listView = new ListView<>(blocks);

        listView.setFixedCellSize(24);
        listView.getStyleClass().add("picker-listview");

        // Display each IncludeView as "Label (name)"
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PersonalizationBlock item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : String.format("%s%s (%s)", item.getFolder().getFullName(), item.getLabel(), item.getName()));
            }
        });

        if (!blocks.isEmpty()) {
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
                PersonalizationBlock selected = listView.getSelectionModel().getSelectedItem();
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