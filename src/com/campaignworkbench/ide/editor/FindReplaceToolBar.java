package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;

public class FindReplaceToolBar implements IJavaFxNode {
    private ToolBar toolBar;
    private final EditorTab editorTab;
    private final TextField findField;

    public FindReplaceToolBar(EditorTab editorTab) {
        this.editorTab = editorTab;
        toolBar = new ToolBar();

        // Find toolbar
        Label findLabel = new Label("Find:");
        findField = new TextField();
        Button findButton = UiUtil.createMiniToolbarButton("", "Find all", IdeIcon.FIND_START, true, "positive-icon", 20, true, _ -> findHandler());
        Button clearFindButton = UiUtil.createMiniToolbarButton("", "Clear", IdeIcon.FIND_CLEAR, true, "negative-icon", 20, true, _ -> clearFindHandler());
        toolBar = new ToolBar(findLabel, findField, findButton, clearFindButton);
        toolBar.getStyleClass().add("small-toolbar");
    }

    private void findHandler() {
        String findText = findField.getText();
        if (!findText.isEmpty()) {
            editorTab.getEditor().find(findText);
        }
    }

    private void clearFindHandler() {
        findField.setText("");
        editorTab.getEditor().find(null);
    }

    @Override
    public Node getNode() {
        return toolBar;
    }

}
