package com.campaignworkbench.ide.toolbars;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.Template;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.function.Consumer;

public class RunToolBar implements IJavaFxNode {
    private final ToolBar toolBar;
    private final Button runButton;
    private final ComboBox<Template> templateDropDown;
    private final Consumer<Template> runConsumer;

    public RunToolBar(Consumer<Template> runConsumer) {

        this.runConsumer = runConsumer;
        runButton = UiUtil.createToolbarButton("", "Run template", IdeIcon.RUN_TEMPLATE, true, "positive-icon", 20, false, _ -> runButtonHandler());

        Label templateLabel = new Label("Template: ");
        templateDropDown = new ComboBox<>();
        templateDropDown.getStyleClass().add("template-dropdown");

        runButton.disableProperty().bind(
                templateDropDown.valueProperty().isNull()
        );

        // Optional: choose how to display Template objects
        templateDropDown.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Template item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getBaseFileName());
            }
        });

        templateDropDown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Template item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getBaseFileName());
            }
        });


        toolBar = new ToolBar(templateLabel, templateDropDown, runButton);
        toolBar.getStyleClass().add("large-toolbar");
    }

    public void setTemplateObservableList(ObservableList<Template> templateObservableList) {
        // Bind the ComboBox directly to the workspace list
        templateDropDown.setItems(templateObservableList);
    }

    private void runButtonHandler() {
        Template selectedTemplate = templateDropDown.getSelectionModel().getSelectedItem();
        if (selectedTemplate != null) {
            runConsumer.accept(templateDropDown.getValue());
        }
    }

    @Override
    public Node getNode() {
        return toolBar;
    }
}
