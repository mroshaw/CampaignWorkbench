package com.campaignworkbench.ide.dialogs;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.stage.Window;

import java.util.Optional;

public final class YesNoPopupDialog extends BasePopupDialog {

    public enum YesNo {
        YES, NO
    }

    private YesNoPopupDialog() {}

    public static YesNo show(
            Window owner,
            String title,
            String message

    ) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message, owner);

        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No");

        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == yes
                ? YesNo.YES
                : YesNo.NO;
    }
}