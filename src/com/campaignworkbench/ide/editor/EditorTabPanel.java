package com.campaignworkbench.ide.editor;

import com.campaignworkbench.ide.IJavaFxNode;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.workspace.WorkspaceFile;
import com.campaignworkbench.workspace.WorkspaceFileType;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a tabbed panel of Editor tabs
 */
public class EditorTabPanel implements IJavaFxNode {

    private final TabPane tabPane;
    private final ErrorReporter errorReporter;
    /**
     * Constructor
     *
     * @param tabChangedListener action to call when the tab is changed
     */
    public EditorTabPanel(ChangeListener<Tab> tabChangedListener, ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        tabPane = new TabPane();
        tabPane.setMinHeight(0);

        tabPane.getSelectionModel().selectedItemProperty().addListener(tabChangedListener);
        tabPane.getSelectionModel().selectedItemProperty().addListener((_, _, newTab) -> refreshTabEditor(newTab));

        // Set style class
        tabPane.getStyleClass().add("editor-tab-panel");
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

    /**
     * @param path The path to the file
     * @return true if the file is already open in a tab
     */
    public boolean isOpened(Path path) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab) {
                if (editorTab.getFile().equals(path)) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Gets the window underlying the tab panel
     *
     * @return the underlying tab window
     */
    public Window getWindow() {
        return tabPane.getScene().getWindow();
    }

    /**
     * Adds a new Editor Tab to the Tab Panel
     *
     * @param workspaceFile workspace file to be edited in the tab
     */
    public void addEditorTab(WorkspaceFile workspaceFile) {
        EditorTab existingTab = getExistingTab(workspaceFile);
        if(existingTab != null) {
            // Refresh and set focus on the tab
            getSelected().refreshText();
            tabPane.getSelectionModel().select(existingTab);
            return;
        }

        EditorTab tab = new EditorTab(workspaceFile, errorReporter);
        tab.setClosable(true);
        // Backups should be read only
        if(workspaceFile.getFileType() == WorkspaceFileType.BACKUP) {
            tab.setEditable(false);
        }
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    public WorkspaceFile getSelectedWorkspaceFile() {
        return getSelected().getWorkspaceFile();
    }

    /**
     * @return the file from the currently selected tab
     */
    public WorkspaceFile getSelectedFile() {
        return getSelected().getFile();
    }

    /**
     * @return the code as text from the currently selected tab
     */
    public String getSelectedText() {
        return getSelected().getEditorText();
    }

    /**
     * @return the code in the currently selected tab
     */
    public String getSelectedFileName() {
        return getSelectedFile().getFileName().toString();
    }

    public boolean isSelectedTemplateAndReady() {
        EditorTab selectedTab = getSelected();
        return selectedTab.isTemplateTab() && selectedTab.isContextSet();
    }

    private EditorTab getSelected() {
        return (EditorTab) tabPane.getSelectionModel().getSelectedItem();
    }

    /**
     * @return true if this tab is the currently selected tab
     */
    public boolean isSelected() {
        return getSelected() != null;
    }

    public void saveSelectedTab() {
        getSelected().saveFile();
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
