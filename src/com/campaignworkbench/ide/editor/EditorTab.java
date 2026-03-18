package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IdeException;
import com.campaignworkbench.ide.dialogs.YesNoCancelPopupDialog;
import com.campaignworkbench.ide.editor.richtextfx.RichTextFXEditor;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.workspace.WorkspaceFile;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.reactfx.Subscription;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Implements a Tab containing a code editor
 */
public final class EditorTab extends Tab {

    private final WorkspaceFile workspaceFile;
    private final ICodeEditor editor;

    private boolean isTextDirty;

    private final ErrorReporter errorReporter;

    /**
     * Constructor
     *
     * @param workspaceFile that the editor is editing
     */
    public EditorTab(EditorTabPanel editorTabPanel, WorkspaceFile workspaceFile, ErrorReporter errorReporter, SimpleBooleanProperty connectedObservable, Supplier<Boolean> connectedStateSupplier,
                     Consumer<WorkspaceFile> refreshConsumer, Consumer<WorkspaceFile> pushConsumer) {
        this.workspaceFile = workspaceFile;
        this.errorReporter = errorReporter;

        FormatToolBar formatToolBar = new FormatToolBar(this, editorTabPanel::closeAllTabs);
        CampaignToolBar campaignToolBar = new CampaignToolBar(this, connectedObservable, connectedStateSupplier, refreshConsumer, pushConsumer);
        FindReplaceToolBar findReplaceToolBar = new FindReplaceToolBar(this);

        // Set the tab title
        updateTabText();

        // Combine the toolbars
        HBox toolsContainer = new HBox();
        toolsContainer.getChildren().addAll(formatToolBar.getNode(), campaignToolBar.getNode(),findReplaceToolBar.getNode());
        HBox.setHgrow(campaignToolBar.getNode(), Priority.ALWAYS);

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

    public void saveFile()  {
        try {
            String content = editor.getText();
            workspaceFile.saveWorkspaceFileContent(content);
            isTextDirty = false;
            updateTabText();
        } catch (IdeException ex) {
            errorReporter.reportError("An error occurred while saving the file: " + workspaceFile.getFileName(), ex, true);
        }
    }

    public void setEditable(boolean isEditable) {
        editor.setEditable(isEditable);
    }

    public void refreshText() {
        editor.setText(workspaceFile.getWorkspaceFileContent());
    }

    private void editorTextChangedHandler(String newText) {
        isTextDirty = true;
        updateTabText();
    }

    private void updateTabText() {
        String tabName = workspaceFile.getFileName();
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

    private void tabClosedHandler(Event event) {
        if (isTextDirty) {
            YesNoCancelPopupDialog.YesNoCancel result = YesNoCancelPopupDialog.show(getTabPane().getScene().getWindow(), "Save changes?", "The file contents have changed. Do you want to save?");
            if (result == YesNoCancelPopupDialog.YesNoCancel.CANCEL) {
                event.consume();
            }
            if (result == YesNoCancelPopupDialog.YesNoCancel.YES) {
                saveFile();
            }
        }
    }

    /**
     * Refresh the context of the tab
     */
    public void refreshEditor() {
        editor.refreshContent();
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
}
