package com.campaignworkbench.ide.dialogs;


import javafx.scene.control.Alert;
import javafx.stage.Stage;

abstract class BasePopupDialog {

    protected static Alert createAlert(
            Alert.AlertType type,
            String title,
            String message,
            Stage owner
    ) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }
}