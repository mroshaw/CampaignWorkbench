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

public class WorkspaceToolBar implements IJavaFxNode {

    private ToolBar toolBar;

    public WorkspaceToolBar(EventHandler<ActionEvent> openWorkspaceHandler,
                            EventHandler<ActionEvent> newWorkspaceHandler,
                            EventHandler<ActionEvent> closeWorkspaceHandler,
                            EventHandler<ActionEvent> closeEditorTabsHandler) {

        Button openWorkspaceButton = UiUtil.createToolbarButton("", "Open Workspace", IdeIcon.OPEN_WORKSPACE, true, "workspace-icon", 20, true, openWorkspaceHandler);
        Button newWorkspaceButton = UiUtil.createToolbarButton("", "New Workspace", IdeIcon.NEW_WORKSPACE, true, "positive-icon", 20, true, newWorkspaceHandler);
        Button closeWorkspaceButton = UiUtil.createToolbarButton("", "Close Workspace", IdeIcon.CLOSE_WORKSPACE, true, "negative-icon", 20, true, closeWorkspaceHandler);

        toolBar = new ToolBar(
                openWorkspaceButton,
                newWorkspaceButton,
                closeWorkspaceButton,
                new Separator(Orientation.VERTICAL));

        toolBar.getStyleClass().add("large-toolbar");
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
