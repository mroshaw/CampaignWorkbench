package com.campaignworkbench.ide.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

public final class YesNoCancelPopupDialog extends BasePopupDialog {

    public enum YesNoCancel {
        YES, NO, CANCEL
    }

    private YesNoCancelPopupDialog() {}

    public static YesNoCancel show(
            String title,
            String message,
            Stage owner
    ) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message, owner);

        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(yes, no, cancel);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() == cancel) {
            return YesNoCancel.CANCEL;
        }

        return result.get() == yes
                ? YesNoCancel.YES
                : YesNoCancel.NO;
    }
}