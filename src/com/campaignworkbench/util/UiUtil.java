package com.campaignworkbench.util;

import com.campaignworkbench.ide.CampaignWorkbenchIDE;
import com.campaignworkbench.ide.IdeException;
import com.campaignworkbench.ide.icons.IdeIcon;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

public class UiUtil {

    public static Button createToolbarButton(String buttonText, String toolTipText, IdeIcon icon, boolean filled, String styleClass, Integer resolution, boolean defaultState, EventHandler<ActionEvent> eventHandler) {
        return createButton(buttonText, toolTipText, icon, filled, styleClass, resolution, 28, defaultState, eventHandler);
    }

    public static Button createMiniToolbarButton(String buttonText, String toolTipText, IdeIcon icon, boolean filled, String styleClass, Integer resolution, boolean defaultState, EventHandler<ActionEvent> eventHandler) {
        return createButton(buttonText, toolTipText, icon, filled, styleClass, resolution, 20, defaultState, eventHandler);
    }

    public static ToggleButton createMiniToolbarToggleButton(String buttonText, String toolTipText, IdeIcon icon, boolean filled, String styleClass, Integer resolution, boolean defaultState, Consumer<Boolean> toggleStateConsumer) {
        return createToggleButton(buttonText, toolTipText, icon, filled, styleClass, resolution, 20, defaultState, toggleStateConsumer);
    }

    public static String getStylesFromStyleSheet(String styleSheet) {
        URL styleSheetUrl = CampaignWorkbenchIDE.class.getResource(styleSheet);
        if( styleSheetUrl != null ) {
            return styleSheetUrl.toExternalForm();
        }
        else {
            throw new IdeException("Unable to locate style sheet: " + styleSheet, null);
        }
    }

    public static ToggleButton createToggleButton(String buttonText, String toolTipText, IdeIcon icon, boolean filled, String styleClass, Integer resolution, Integer size, boolean defaultState, Consumer<Boolean> toggleStateConsumer) {
        SVGPath iconNode = (SVGPath) icon.getIcon(resolution, styleClass, filled);
        iconNode.getStyleClass().add("button-icon");

        // Scale the SVGPath to the desired size using a view box approach
        double scale = size.doubleValue() / resolution.doubleValue();
        iconNode.setScaleX(scale);
        iconNode.setScaleY(scale);

        // Wrap in a fixed-size pane so layout bounds match the desired size
        javafx.scene.layout.StackPane iconWrapper = new javafx.scene.layout.StackPane(iconNode);
        iconWrapper.setPrefSize(size, size);
        iconWrapper.setMaxSize(size, size);
        iconWrapper.setMinSize(size, size);

        ToggleButton newToggleButton = Objects.equals(buttonText, "") ?
                new ToggleButton(null, iconWrapper) : new ToggleButton(buttonText, iconWrapper);
        newToggleButton.setTooltip(new Tooltip(toolTipText));
        newToggleButton.selectedProperty().addListener((_, _, selected) -> toggleStateConsumer.accept(selected));
        newToggleButton.setDisable(!defaultState);
        return newToggleButton;
    }

    public static Button createButton(String buttonText, String toolTipText, IdeIcon icon,
                                      boolean filled, String styleClass, Integer resolution, Integer size,
                                      boolean defaultState, EventHandler<ActionEvent> eventHandler) {

        SVGPath iconNode = (SVGPath) icon.getIcon(resolution, styleClass, filled);
        iconNode.getStyleClass().add("button-icon");

        // Scale the SVGPath to the desired size using a view box approach
        double scale = size.doubleValue() / resolution.doubleValue();
        iconNode.setScaleX(scale);
        iconNode.setScaleY(scale);

        // Wrap in a fixed-size pane so layout bounds match the desired size
        javafx.scene.layout.StackPane iconWrapper = new javafx.scene.layout.StackPane(iconNode);
        iconWrapper.setPrefSize(size, size);
        iconWrapper.setMaxSize(size, size);
        iconWrapper.setMinSize(size, size);

        Button newButton = Objects.equals(buttonText, "") ?
                new Button(null, iconWrapper) : new Button(buttonText, iconWrapper);
        newButton.setTooltip(new Tooltip(toolTipText));
        newButton.setOnAction(eventHandler);
        newButton.setDisable(!defaultState);
        return newButton;
    }
}
