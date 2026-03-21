package com.campaignworkbench.ide.toolbars;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;

public class WorkspaceToolBar implements IJavaFxNode {

    private final ToolBar toolBar;

    public WorkspaceToolBar(Runnable openWorkspaceHandler,
                            Runnable newWorkspaceHandler,
                            Runnable closeWorkspaceHandler) {

        Button openWorkspaceButton = UiUtil.createMiniToolbarButton("", "Open Workspace", IdeIcon.OPEN_WORKSPACE, true, "plain-icon", 20, true,_ -> openWorkspaceHandler.run());
        Button newWorkspaceButton = UiUtil.createMiniToolbarButton("", "New Workspace", IdeIcon.NEW_WORKSPACE, true, "plain-icon", 20, true, _ -> newWorkspaceHandler.run());
        Button closeWorkspaceButton = UiUtil.createMiniToolbarButton("", "Close Workspace", IdeIcon.CLOSE_WORKSPACE, true, "plain-icon", 20, true,_ -> closeWorkspaceHandler.run());

        toolBar = new ToolBar(
                openWorkspaceButton,
                newWorkspaceButton,
                closeWorkspaceButton,
                new Separator(Orientation.VERTICAL));

        toolBar.getStyleClass().add("small-toolbar");
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
