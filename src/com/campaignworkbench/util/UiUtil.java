package com.campaignworkbench.util;

import com.campaignworkbench.ide.icons.IdeIcon;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class UiUtil {

    public static Button createButton(String buttonText, String toolTipText, IdeIcon icon, boolean filled, String styleClass, Integer resolution, Integer size, boolean defaultState, EventHandler eventHandler) {
        Node iconNode = icon.getIcon(resolution, styleClass, filled);
        float scale = size.floatValue() / resolution.floatValue();
        iconNode.setScaleX(scale);
        iconNode.setScaleY(scale);
        Button newButton = new Button(buttonText, iconNode);
        newButton.setTooltip(new Tooltip(toolTipText));
        newButton.setOnAction(eventHandler);
        newButton.setDisable(!defaultState);
        return newButton;
    }
}
