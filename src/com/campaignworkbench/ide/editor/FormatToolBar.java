package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;

public class FormatToolBar implements IJavaFxNode {

    private final ToolBar toolBar;
    private final ToggleButton toggleWrapButton;
    private final EditorTab editorTab;

    public FormatToolBar(EditorTab editorTab, Runnable closeAllTabsHandler) {
        this.editorTab = editorTab;

        // Format toolbar
        Button formatButton = UiUtil.createMiniToolbarButton("", "Format code", IdeIcon.FORMAT_CODE, true, "positive-icon", 20, true, _ -> editorTab.getEditor().formatCode(2));
        Button foldAllButton = UiUtil.createMiniToolbarButton("", "Fold all", IdeIcon.FOLD_ALL, true, "positive-icon", 20, true, _ ->  editorTab.getEditor().foldAll());
        Button unfoldAllButton = UiUtil.createMiniToolbarButton("", "Unfold all", IdeIcon.UNFOLD_ALL, true, "positive-icon", 20, true, _ -> editorTab.getEditor().unfoldAll());
        toggleWrapButton = UiUtil.createMiniToolbarToggleButton("", "Toggle wrap", IdeIcon.WRAP_TEXT, true, "positive-icon", 20, true, _ -> toggleWrapHandler());
        Button closeEditorTabsButton = UiUtil.createMiniToolbarButton("", "Close all editor tabs", IdeIcon.CLOSE_ALL_TABS, true, "negative-icon", 20, true, _-> closeAllTabsHandler.run());
        toolBar = new ToolBar(formatButton, foldAllButton, unfoldAllButton, toggleWrapButton, new Separator(Orientation.VERTICAL), closeEditorTabsButton, new Separator(Orientation.VERTICAL));
        toolBar.getStyleClass().add("small-toolbar");
    }

    private void toggleWrapHandler() {
        editorTab.getEditor().setWrap(toggleWrapButton.isSelected());
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
