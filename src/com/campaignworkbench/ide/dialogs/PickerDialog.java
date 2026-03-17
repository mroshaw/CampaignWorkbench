package com.campaignworkbench.ide.dialogs;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

import java.util.Optional;
import java.util.function.Function;

public abstract class PickerDialog<T> {
    protected Optional<T> showDialog(
            Window owner,
            String title,
            String headerText,
            ObservableList<T> items,
            Function<T, String> labelProvider,
            double width,
            double height
    ) {

        Dialog<T> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);

        dialog.setOnShown(event -> {
            owner.setWidth(width);
            owner.setHeight(height);

            owner.setX(owner.getX() + (owner.getWidth() - owner.getWidth()) / 2);
            owner.setY(owner.getY() + (owner.getHeight() - owner.getHeight()) / 2);
        });

        ButtonType pickButtonType =
                new ButtonType("Pick", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes()
                .addAll(pickButtonType, ButtonType.CANCEL);

        ListView<T> listView = new ListView<>(items);
        listView.setFixedCellSize(24);
        listView.getStyleClass().add("picker-listview");

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labelProvider.apply(item));
            }
        });

        if (!items.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        }

        Node pickButton = dialog.getDialogPane().lookupButton(pickButtonType);
        pickButton.setDisable(listView.getSelectionModel().getSelectedItem() == null);

        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> pickButton.setDisable(newVal == null)
        );

        listView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                T selected = listView.getSelectionModel().getSelectedItem();
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