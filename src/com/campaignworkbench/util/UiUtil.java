package com.campaignworkbench.util;

import com.campaignworkbench.ide.CampaignWorkbenchIDE;
import com.campaignworkbench.ide.IdeException;
import com.campaignworkbench.ide.icons.IdeIcon;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.util.Objects;

public class UiUtil {

    public static String getStylesFromStyleSheet(String styleSheet) {
        URL styleSheetUrl = CampaignWorkbenchIDE.class.getResource(styleSheet);
        if( styleSheetUrl != null ) {
            return styleSheetUrl.toExternalForm();
        }
        else {
            throw new IdeException("Unable to locate style sheet: " + styleSheet, null);
        }
    }

    public static ToggleButton createToggleButton(String buttonText, String toolTipText, IdeIcon icon, boolean filled, String styleClass, Integer resolution, Integer size, boolean defaultState, EventHandler eventHandler) {
        Node iconNode = icon.getIcon(resolution, styleClass, filled);
        iconNode.getStyleClass().add("button-icon");
        float scale = size.floatValue() / resolution.floatValue();
        iconNode.setScaleX(scale);
        iconNode.setScaleY(scale);
        ToggleButton newButton = Objects.equals(buttonText, "") ? new ToggleButton(null, iconNode) : new ToggleButton(buttonText, iconNode);
        newButton.setTooltip(new Tooltip(toolTipText));
        newButton.setOnAction(eventHandler);
        newButton.setDisable(!defaultState);
        return newButton;
    }

    public static Button createButton(String buttonText, String toolTipText, IdeIcon icon, boolean filled, String styleClass, Integer resolution, Integer size, boolean defaultState, EventHandler eventHandler) {
        Node iconNode = icon.getIcon(resolution, styleClass, filled);
        iconNode.getStyleClass().add("button-icon");
        float scale = size.floatValue() / resolution.floatValue();
        iconNode.setScaleX(scale);
        iconNode.setScaleY(scale);
        Button newButton = Objects.equals(buttonText, "") ? new Button(null, iconNode) : new Button(buttonText, iconNode);
        newButton.setTooltip(new Tooltip(toolTipText));
        newButton.setOnAction(eventHandler);
        newButton.setDisable(!defaultState);
        return newButton;
    }
}
