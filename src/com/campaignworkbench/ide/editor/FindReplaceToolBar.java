package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;

import java.util.function.Consumer;

public class FindReplaceToolBar implements IJavaFxNode {
    private ToolBar toolBar;
    private final Consumer<String> findHandler;
    private final Runnable clearHandler;
    private final TextField findField;

    public FindReplaceToolBar(Consumer<String> findHandler, Runnable clearHandler) {
        this.findHandler = findHandler;
        this.clearHandler = clearHandler;
        toolBar = new ToolBar();

        Label findLabel = new Label("Find:");
        findField = new TextField();
        Button findButton = UiUtil.createMiniToolbarButton("", "Find all", IdeIcon.FIND_START, true, "plain-icon", 20, true, _ -> handleFind());
        Button clearFindButton = UiUtil.createMiniToolbarButton("", "Clear", IdeIcon.FIND_CLEAR, true, "negative-icon", 20, true, _ -> handleClear());
        toolBar = new ToolBar(findLabel, findField, findButton, clearFindButton);
        toolBar.getStyleClass().add("small-toolbar");
    }

    private void handleFind() {
        String findText = findField.getText();
        if (!findText.isEmpty()) {
            findHandler.accept(findText);
        }
    }

    private void handleClear() {
        findField.setText("");
        clearHandler.run();
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}