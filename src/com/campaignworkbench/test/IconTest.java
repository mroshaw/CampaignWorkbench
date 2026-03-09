package com.campaignworkbench.test;

import com.campaignworkbench.ide.IdeTheme;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class IconTest extends Application {

    @Override
    public void start(Stage stage) {

        Label label = new Label("JavaFX is working.");

        // Test large icons
        HBox largeIconBox = new HBox(20);
        Node workflowIcon = IdeIcon.WORKSPACE.getIcon(24, "positive-icon", true);
        Node templateIcon = IdeIcon.TEMPLATE.getIcon(24, "positive-icon", true);
        Node moduleIcon = IdeIcon.MODULE.getIcon(24, "small-icon", true);
        Node blockIcon = IdeIcon.BLOCK.getIcon(24, "small-icon", true);
        Node contextIcon = IdeIcon.CONTEXT.getIcon(24, "small-icon", true);

        largeIconBox.getChildren().addAll(workflowIcon, templateIcon, moduleIcon, blockIcon, contextIcon);

        // Test small icons
        HBox smallIconBox = new HBox(20);
        Node workflowSmallIcon = IdeIcon.WORKSPACE.getIcon(16, "large-icon", true);
        smallIconBox.getChildren().addAll(workflowSmallIcon);

        // Button icons
        HBox buttonBox = new HBox(20);
        Button workspaceButton = UiUtil.createButton("", "Open Workspace", IdeIcon.OPEN_WORKSPACE, true, "workspace-icon", 28, 28, true, null);
        buttonBox.getChildren().addAll(workspaceButton);

        VBox displayBox = new VBox(10);
        displayBox.getChildren().addAll(label, largeIconBox, smallIconBox, buttonBox);
        Scene scene = new Scene(displayBox, 400, 200);
        scene.getStylesheets().add(IdeTheme.DARK.getIdeStyleSheet());


        stage.setTitle("JavaFX Smoke Test");
        stage.setScene(scene);
        stage.show();
    }

    static void main(String[] args) {
        launch(args);
    }
}