package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.workspace.WorkspaceFile;
import com.campaignworkbench.workspace.WorkspaceFileType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implements a tabbed panel of Editor tabs
 */
public class EditorTabPanel implements IJavaFxNode {

    private final TabPane tabPane;
    private final ErrorReporter errorReporter;
    private final SimpleBooleanProperty connectedObservable;

    Consumer<WorkspaceFile> refreshConsumer;
    Consumer<WorkspaceFile> pushConsumer;

    /**
     * Constructor
     *
     * @param tabChangedListener action to call when the tab is changed
     */
    public EditorTabPanel(ChangeListener<Tab> tabChangedListener, ErrorReporter errorReporter,
                          SimpleBooleanProperty connectedObservable,
                          Consumer<WorkspaceFile> refreshConsumer, Consumer<WorkspaceFile> pushConsumer) {
        this.connectedObservable = connectedObservable;
        this.errorReporter = errorReporter;
        this.pushConsumer = pushConsumer;
        this.refreshConsumer = refreshConsumer;

        tabPane = new TabPane();
        tabPane.setMinHeight(0);

        if (tabChangedListener != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener(tabChangedListener);
        }

        tabPane.getSelectionModel().selectedItemProperty().addListener((_, _, newTab) -> refreshTabEditor(newTab));

        // Set style class
        tabPane.getStyleClass().add("editor-tab-panel");
    }

    public void setConsumers(Consumer<WorkspaceFile> refreshConsumer, Consumer<WorkspaceFile> pushConsumer) {
        this.refreshConsumer = refreshConsumer;
        this.pushConsumer = pushConsumer;
    }

    private void refreshTabEditor(Tab tab) {
        if (tab instanceof EditorTab editorTab) {
            editorTab.refreshEditor();
        }
    }

    /**
     * Finds and selects a tab by its file path, or opens it if not found.
     * Then jumps to the specified line.
     *
     * @param path The path to the file
     * @param line The line number to jump to (1-indexed)
     */
    public void openFileAndGoToLine(Path path, int line) {
        EditorTab targetTab = null;
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab) {
                if (editorTab.getFile().equals(path)) {
                    targetTab = editorTab;
                    break;
                }
            }
        }

        if (targetTab == null) {
            return;
        }

        tabPane.getSelectionModel().select(targetTab);
        targetTab.getEditor().gotoLine(line);
    }

    private EditorTab getExistingTab(WorkspaceFile workspaceFile) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab) {
                if (editorTab.getFile().equals(workspaceFile)) {
                    return editorTab;
                }
            }
        }
        return null;
    }

     // Gets the window underlying the tab panel
    public Window getWindow() {
        return tabPane.getScene().getWindow();
    }


    // Adds a new Editor Tab to the Tab Panel
    public void addEditorTab(WorkspaceFile workspaceFile) {
        EditorTab existingTab = getExistingTab(workspaceFile);
        if(existingTab != null) {
            // Refresh and set focus on the tab
            getSelected().refreshText();
            tabPane.getSelectionModel().select(existingTab);
            return;
        }

        EditorTab tab = new EditorTab(workspaceFile, errorReporter, connectedObservable,
                refreshConsumer, pushConsumer, this::closeAllTabs);
        tab.setClosable(true);
        // Backups should be read only
        if(workspaceFile.getFileType() == WorkspaceFileType.BACKUP) {
            tab.setEditable(false);
        }
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private EditorTab getSelected() {
        return (EditorTab) tabPane.getSelectionModel().getSelectedItem();
    }

    public void saveSelectedTab() {
        if(getSelected() != null) {
            getSelected().saveFile();
        }
    }

    public void saveSelectedTabAs() {

    }

    public void insertTextIntoSelected(String text) {
        EditorTab selectedTab = getSelected();
        if(selectedTab != null) {
            getSelected().insertTextAtCaret(text);
        }
    }

    public void saveAllTabs() {
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab) {
                editorTab.saveFile();
            }
        }
    }

    public void closeAllTabs() {
        List<Tab> tabs = new ArrayList<>(tabPane.getTabs()); // copy to avoid ConcurrentModificationException
        for (Tab tab : tabs) {
            if (tab.isClosable()) {
                // Fire the close request event
                Event closeRequestEvent = new Event(Tab.TAB_CLOSE_REQUEST_EVENT);
                Event.fireEvent(tab, closeRequestEvent);

                // Only close if the event wasn't consumed (i.e. not cancelled)
                if (!closeRequestEvent.isConsumed()) {
                    tabPane.getTabs().remove(tab);
                    Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
                }
            }
        }
    }

    @Override
    public Node getNode() {
        return tabPane;
    }
}
