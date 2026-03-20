package com.campaignworkbench.ide.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

public class PasswordInputDialog {

    public static Optional<char[]> show(Window owner, String title, String headerText, String contentText) {

        Dialog<char[]> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setResizable(false);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        Label messageLabel = new Label(headerText);
        messageLabel.setWrapText(true);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(contentText);
        passwordField.setPrefColumnCount(28);

        VBox content = new VBox(10, messageLabel, passwordField);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);

        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        passwordField.textProperty().addListener((_, _, newVal) ->
                okButton.setDisable(newVal == null || newVal.trim().isEmpty()));

        dialog.setOnShown(_ -> passwordField.requestFocus());

        dialog.setResultConverter(buttonType ->
                buttonType == okButtonType ? passwordField.getText().toCharArray() : null);

        return dialog.showAndWait();
    }
}