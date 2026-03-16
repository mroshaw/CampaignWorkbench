package com.campaignworkbench.ide.results;

import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebPreviewTab extends Tab {
    private final WebEngine webEngine;
    private final WebView webView;
    private String previewContent;

    public WebPreviewTab(String text) {
        super(text);
        webView = new WebView();
        webEngine = webView.getEngine();

        Button backButton = UiUtil.createMiniToolbarButton("",  "Go back", IdeIcon.BACK, true, "plain-icon", 20, true, _ -> goBack());
        Button forwardButton = UiUtil.createMiniToolbarButton("",  "Go forwards", IdeIcon.FORWARD, true, "plain-icon", 20, true, _ -> goForward());
        Button homeButton = UiUtil.createMiniToolbarButton("",  "Go home", IdeIcon.HOME, true, "plain-icon", 20, true, _ -> goHome());
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("mini-toolbar");
        toolBar.getItems().addAll(backButton, forwardButton, homeButton);

        VBox container = new VBox(toolBar, webView);

        setContent(container);
    }

    private void goBack() {
        Platform.runLater(() -> {
            webEngine.executeScript("history.back()");
        });
    }

    private void goForward() {
        Platform.runLater(() -> {
            webEngine.executeScript("history.forward()");
        });
    }

    private void goHome() {
        setContent(previewContent);
    }

    public void setCursor(Cursor cursor) {
        webView.setCursor(cursor);
    }

    public void setContent(String content) {
        previewContent = content;
        Platform.runLater(() -> {
        webEngine.loadContent(content);
        });
    }

    public void executeScript(String script) {
        webEngine.executeScript(script);
    }

}
