package com.campaignworkbench.ide.toolbars;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;

/**
 * Implements a button toolbar for use within the IDE User Interface
 */
public class MainToolBar implements IJavaFxNode {

    private final ToolBar toolBar;
    private final Button runButton;

    /**
     * Constructor
     *
     * @param openWorkspaceHandler - action to take when the open workspace button is clicked
     * @param runHandler           - action to take when the run button is clicked
     */
    public MainToolBar(
            EventHandler<ActionEvent> openWorkspaceHandler,
            EventHandler<ActionEvent> newWorkspaceHandler,
            EventHandler<ActionEvent> closeWorkspaceHandler,
            EventHandler<ActionEvent> closeEditorTabsHandler,
            EventHandler<ActionEvent> runHandler
    ) {
        Button openWorkspaceButton = UiUtil.createToolbarButton("", "Open Workspace", IdeIcon.OPEN_WORKSPACE, true, "workspace-icon", 20, true, openWorkspaceHandler);
        Button newWorkspaceButton = UiUtil.createToolbarButton("", "New Workspace", IdeIcon.NEW_WORKSPACE, true, "positive-icon", 20, true, newWorkspaceHandler);
        Button closeWorkspaceButton = UiUtil.createToolbarButton("", "Close Workspace", IdeIcon.CLOSE_WORKSPACE, true, "negative-icon", 20, true, closeWorkspaceHandler);
        Button closeEditorTabsButton = UiUtil.createToolbarButton("", "Close all editor tabs", IdeIcon.CLOSE_ALL_TABS, true, "negative-icon", 20, true, closeEditorTabsHandler);
        runButton = UiUtil.createToolbarButton("", "Run template", IdeIcon.RUN_TEMPLATE, true, "positive-icon", 20, false, runHandler);

        toolBar = new ToolBar(
                openWorkspaceButton,
                newWorkspaceButton,
                closeWorkspaceButton,
                new Separator(Orientation.VERTICAL),
                closeEditorTabsButton,
                new Separator(Orientation.VERTICAL),
                runButton
        );

        toolBar.getStyleClass().add("large-toolbar");
    }

    /**
     * @param state true or false state of the run button
     */
    public void setRunButtonState(boolean state) {
        runButton.setDisable(!state);
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
