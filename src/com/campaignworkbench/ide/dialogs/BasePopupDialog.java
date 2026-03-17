package com.campaignworkbench.ide.dialogs;


import javafx.scene.control.Alert;
import javafx.stage.Window;

abstract class BasePopupDialog {

    protected static Alert createAlert(
            Alert.AlertType type,
            String title,
            String message,
            Window owner
    ) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }
}