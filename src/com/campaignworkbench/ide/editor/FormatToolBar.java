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

import java.util.function.Consumer;

public class FormatToolBar implements IJavaFxNode {

    private final ToolBar toolBar;

    public FormatToolBar(Runnable formatCode, Runnable foldAll, Runnable unfoldAll,
                         Consumer<Boolean> toggleWrap, Runnable closeAllTabs) {

        Button formatButton = UiUtil.createMiniToolbarButton("", "Format code", IdeIcon.FORMAT_CODE, true, "plain-icon", 20, true, _ -> formatCode.run());
        Button foldAllButton = UiUtil.createMiniToolbarButton("", "Fold all", IdeIcon.FOLD_ALL, true, "plain-icon", 20, true, _ -> foldAll.run());
        Button unfoldAllButton = UiUtil.createMiniToolbarButton("", "Unfold all", IdeIcon.UNFOLD_ALL, true, "plain-icon", 20, true, _ -> unfoldAll.run());
        ToggleButton wrapButton = UiUtil.createMiniToolbarToggleButton("", "Toggle wrap", IdeIcon.WRAP_TEXT, true, "plain-icon", 20, true, toggleWrap);
        Button closeEditorTabsButton = UiUtil.createMiniToolbarButton("", "Close all editor tabs", IdeIcon.CLOSE_ALL_TABS, true, "plain-icon", 20, true, _ -> closeAllTabs.run());
        toolBar = new ToolBar(formatButton, foldAllButton, unfoldAllButton, wrapButton, new Separator(Orientation.VERTICAL), closeEditorTabsButton, new Separator(Orientation.VERTICAL));
        toolBar.getStyleClass().add("small-toolbar");
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}