package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IdeException;
import com.campaignworkbench.ide.dialogs.YesNoCancelPopupDialog;
import com.campaignworkbench.ide.editor.richtextfx.RichTextFXEditor;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.ide.icons.IdeIcon;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.ContextXml;
import com.campaignworkbench.workspace.WorkspaceContextFile;
import com.campaignworkbench.workspace.WorkspaceFile;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.reactfx.Subscription;

import java.util.Objects;

/**
 * Implements a Tab containing a code editor
 */
public final class EditorTab extends Tab {

    private final ToolBar findReplaceToolBar;
    private final WorkspaceFile workspaceFile;
    private final ICodeEditor editor;
    private final TextField findField;

    private boolean isTextDirty;
    private ToggleButton toggleWrapButton;
    private ErrorReporter errorReporter;

    /**
     * Constructor
     *
     * @param workspaceFile that the editor is editing
     */
    public EditorTab(WorkspaceFile workspaceFile, ErrorReporter errorReporter) {

        this.workspaceFile = workspaceFile;

        // Set the tab title
        updateTabText();

        // Create the toolbar
        // Format toolbar
        Button formatButton = UiUtil.createMiniToolbarButton("", "Format code", IdeIcon.FORMAT_CODE, true, "positive-icon", 20, true, _ -> formatHandler());
        Button foldAllButton = UiUtil.createMiniToolbarButton("", "Fold all", IdeIcon.FOLD_ALL, true, "positive-icon", 20, true, _ -> foldAllHandler());
        Button unfoldAllButton = UiUtil.createMiniToolbarButton("", "Unfold all", IdeIcon.UNFOLD_ALL, true, "positive-icon", 20, true, _ -> unfoldAllHandler());
        toggleWrapButton = UiUtil.createMiniToolbarToggleButton("", "Toggle wrap", IdeIcon.WRAP_TEXT, true, "positive-icon", 20, true, null);
        toggleWrapButton.selectedProperty().addListener(this::toggleWrapHandler);
        ToolBar formatToolBar = new ToolBar(formatButton, foldAllButton, unfoldAllButton, toggleWrapButton);
        formatToolBar.getStyleClass().add("small-toolbar");

        // Find toolbar
        Label findLabel = new Label("Find:");
        findField = new TextField();
        Button findButton = UiUtil.createMiniToolbarButton("", "Find all", IdeIcon.FIND_START, true, "positive-icon", 20, true, _ -> findHandler());
        Button clearFindButton = UiUtil.createMiniToolbarButton("", "Clear", IdeIcon.FIND_CLEAR, true, "negative-icon", 20, true, _ -> clearFindHandler());
        findReplaceToolBar = new ToolBar(findLabel, findField, findButton, clearFindButton);
        findReplaceToolBar.getStyleClass().add("small-toolbar");

        // Combine the toolbars
        HBox toolsContainer = new HBox();
        toolsContainer.getChildren().addAll(formatToolBar, findReplaceToolBar);
        HBox.setHgrow(formatToolBar, Priority.ALWAYS);

        // Create the code editor
        this.editor = new RichTextFXEditor(determineSyntax(workspaceFile));
        BorderPane root = new BorderPane();
        root.setCenter(editor.getNode());
        editor.setText(workspaceFile.getWorkspaceFileContent());
        editor.setCaretAtStart();

        // Attach listener to set file dirty status
        Subscription sub = editor.addTextChangeListener(this::editorTextChangedHandler);

        // Create an assign the main 'container'
        VBox container = new VBox(toolsContainer, editor.getNode());
        VBox.setVgrow(editor.getNode(), Priority.ALWAYS);
        setContent(container);
        container.getStyleClass().add("editor-tab");

        // Set the on-closed handler
        this.setOnCloseRequest(this::tabClosedHandler);
    }

    public void saveFile() throws IdeException {
        String content = editor.getText();
        workspaceFile.saveWorkspaceFileContent(content);
        isTextDirty = false;
        updateTabText();
    }

    public void refreshText() {
        editor.setText(workspaceFile.getWorkspaceFileContent());
    }

    public void setDataContextFile(ContextXml contextFile) {
        if (workspaceFile instanceof WorkspaceContextFile workspaceContextFile) {

            workspaceContextFile.setDataContextFile(contextFile);
            updateTabText();
        } else {
            throw new IdeException("Attempted to set context on a non-context based EditorTab!", null);
        }
    }

    private void editorTextChangedHandler(String newText) {
        isTextDirty = true;
        updateTabText();
    }

    private void updateTabText() {
        String tabName = workspaceFile.getFileName().toString();
        if (isTextDirty) {
            tabName += "*";
        }
        setText(tabName);
    }

    /**
     * @return workspace file associated with this editor tab
     */
    public WorkspaceFile getWorkspaceFile() {
        return workspaceFile;
    }

    /**
     * Get the file associated with this editor tab
     *
     * @return the file associated with this editor tab
     */
    public WorkspaceFile getFile() {
        return workspaceFile;
    }

    public void insertTextAtCaret(String text) {
        editor.insertTextAtCaret(text);
    }

    public boolean isContextSet() {
        if (workspaceFile instanceof WorkspaceContextFile workspaceContextFile) {
            return workspaceContextFile.isDataContextSet();
        } else {
            return false;
        }
    }

    private void tabClosedHandler(Event event) {
        if (isTextDirty) {
            YesNoCancelPopupDialog.YesNoCancel result = YesNoCancelPopupDialog.show("Save changes?", "The file contents have changed. Do you want to save?", (Stage) getTabPane().getScene().getWindow());
            if (result == YesNoCancelPopupDialog.YesNoCancel.CANCEL) {
                event.consume();
            }
            if (result == YesNoCancelPopupDialog.YesNoCancel.YES) {
                saveFile();
            }
        }
    }

    private void formatHandler() {
        try {
            editor.formatCode(2);
        } catch (IdeException ideException) {
            errorReporter.reportError("An error occurred formatting the editor code", ideException, true);
        }
    }

    private void foldAllHandler() {
        editor.foldAll();
    }

    private void unfoldAllHandler() {
        editor.unfoldAll();
    }

    private void toggleWrapHandler(ObservableValue<? extends Boolean> observable, boolean wasSelected, boolean isSelected) {
        editor.setWrap(isSelected);
    }

    private void findHandler() {
        String textToFind = findField.getText();
        if (!Objects.equals(textToFind, "")) {
            editor.find(textToFind);
        }
    }

    private void clearFindHandler() {
        // findField.setText("");
        editor.find("");
    }

    /**
     * Refresh the context of the tab
     */
    public void refreshEditor() {
        editor.refreshContent();
    }

    /**
     * Return the text of this editor tab
     *
     * @return text of the editor tab
     */
    public String getEditorText() {
        return editor.getText();
    }

    /**
     * @return the editor within the editor tab
     */
    public ICodeEditor getEditor() {
        return editor;
    }

    /**
     * Derive the underlying SyntaxType for the editor
     *
     */
    private SyntaxType determineSyntax(WorkspaceFile workspaceFile) {

        return switch (workspaceFile.getFileType()) {
            case TEMPLATE -> SyntaxType.TEMPLATE;
            case BLOCK -> SyntaxType.BLOCK;
            case MODULE -> SyntaxType.MODULE;
            case CONTEXT -> SyntaxType.XML;
            default -> SyntaxType.PLAIN;
        };
    }

    /**
     * Helper for quick check for template type editor
     *
     * @return boolean true if the editor has a template file open
     */
    public boolean isTemplateTab() {
        return workspaceFile.isTemplate();
    }

    public void toggleFind() {
        findReplaceToolBar.setVisible(!findReplaceToolBar.isVisible());
    }

}
