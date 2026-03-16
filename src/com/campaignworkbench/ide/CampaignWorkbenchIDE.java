package com.campaignworkbench.ide;

import com.campaignworkbench.campaignrenderer.*;
import com.campaignworkbench.ide.dialogs.AboutDialog;
import com.campaignworkbench.ide.dialogs.SettingsDialog;
import com.campaignworkbench.ide.editor.EditorTab;
import com.campaignworkbench.ide.editor.EditorTabPanel;
import com.campaignworkbench.ide.logging.ErrorLogPanel;
import com.campaignworkbench.ide.logging.LogPanel;
import com.campaignworkbench.ide.logging.UiErrorReporter;
import com.campaignworkbench.ide.results.OutputTabPanel;
import com.campaignworkbench.ide.themes.IThemeable;
import com.campaignworkbench.ide.themes.IdeTheme;
import com.campaignworkbench.ide.themes.ThemeManager;
import com.campaignworkbench.ide.toolbars.MainMenuBar;
import com.campaignworkbench.ide.toolbars.MainToolBar;
import com.campaignworkbench.ide.workspaceexplorer.WorkspaceExplorer;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.Template;
import com.campaignworkbench.workspace.Workspace;
import com.campaignworkbench.workspace.WorkspaceFile;
import com.campaignworkbench.workspace.WorkspaceFileType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Builds a User Interface for the Campaign Workbench IDE
 */
public class CampaignWorkbenchIDE extends Application implements IThemeable {

    private static final String ideStyleSheet = "/styles/ide_general_styles.css";

    private WorkspaceExplorer workspaceExplorer;
    private MainToolBar toolBar;
    private EditorTabPanel editorTabPanel;
    private UiErrorReporter errorReporter;
    private ErrorLogPanel errorLogPanel;
    private OutputTabPanel outputPanel;
    private EditorTab currentEditorTab;
    private Scene scene;
    private AppSettings appSettings;

    TemplateRenderer templateRenderer = new TemplateRenderer();

    /**
     * Main entry point for the application
     *
     * @param args command line arguments
     */
    static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Campaign Workbench");

        // Set the icon
        Image iconImage = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/app.png")));
        primaryStage.getIcons().add(iconImage);

        // Log panel and Error Reporter
        LogPanel logPanel = new LogPanel("Logs");
        errorLogPanel = new ErrorLogPanel("Errors");
        errorLogPanel.setOnErrorDoubleClicked((workspaceFile, line) -> outputPanel.highlightJsLine(line));

        errorReporter = new UiErrorReporter(logPanel, errorLogPanel);

        appSettings = AppSettingsManager.load();

        // Menu and toolbar
        MainMenuBar menuBar = new MainMenuBar(
                _ -> newWorkspaceHandler(),
                _ -> openWorkspaceHandler(),
                _ -> saveWorkspaceHandler(),
                _ -> closeWorkspaceHandler(),
                _ -> createNewFileHandler(WorkspaceFileType.TEMPLATE),
                _ -> createNewFileHandler(WorkspaceFileType.MODULE),
                _ -> createNewFileHandler(WorkspaceFileType.BLOCK),
                _ -> createNewFileHandler(WorkspaceFileType.CONTEXT),

                _ -> addExistingFileHandler(WorkspaceFileType.TEMPLATE),
                _ -> addExistingFileHandler(WorkspaceFileType.MODULE),
                _ -> addExistingFileHandler(WorkspaceFileType.BLOCK),
                _ -> addExistingFileHandler(WorkspaceFileType.CONTEXT),

                _ -> saveCurrentFileHandler(),
                _ -> saveCurrentFileAsHandler(),

                _ -> showSettings(),

                _ -> toggleFindHandler(),

                _ -> setThemeHandler(IdeTheme.LIGHT),
                _ -> setThemeHandler(IdeTheme.DARK),

                _ -> showAbout(),
                _ -> exitApplication()
        );

        toolBar = new MainToolBar(_ -> openWorkspaceHandler(),
                _ -> newWorkspaceHandler(),
                _ -> closeWorkspaceHandler(),
                _ -> closeEditorTabsHandler(),
                _ -> runTemplate());

        // Workspace Explorer
        workspaceExplorer = new WorkspaceExplorer("Workspace Explorer", this::openFileFromWorkspace, this::workspaceChanged,
                this::insertIntoCodeHandler, errorReporter, appSettings);

        // Editor tabs
        editorTabPanel = new EditorTabPanel((_, _, newTab) -> tabPanelChanged(newTab), errorReporter);

        // Output panes
        outputPanel = new OutputTabPanel();

        SplitPane logSplitPane = new SplitPane();
        logSplitPane.setOrientation(Orientation.HORIZONTAL);
        logSplitPane.getItems().addAll(logPanel.getNode(), errorLogPanel.getNode());
        logSplitPane.setDividerPositions(0.5);

        // --- Split: Workspace Explorer | Editor Tabs ---
        SplitPane workspaceEditorSplit = new SplitPane();
        workspaceEditorSplit.setOrientation(Orientation.HORIZONTAL);
        workspaceEditorSplit.getItems().addAll(
                workspaceExplorer.getNode(),
                editorTabPanel.getNode()
        );
        workspaceEditorSplit.setDividerPositions(0.30);
        SplitPane.setResizableWithParent(workspaceExplorer.getNode(), false);

        // --- Split: (Workspace+Editor) | Preview ---
        SplitPane editorPreviewSplit = new SplitPane();
        editorPreviewSplit.setOrientation(Orientation.HORIZONTAL);
        editorPreviewSplit.getItems().addAll(
                workspaceEditorSplit,
                outputPanel.getNode()
        );
        editorPreviewSplit.setDividerPositions(0.71);
        // SplitPane.setResizableWithParent(previewSplitPane, false);

        VBox topBar = new VBox(menuBar.getNode(), toolBar.getNode());

        SplitPane rootSplitPane = new SplitPane();
        rootSplitPane.setOrientation(Orientation.VERTICAL);
        rootSplitPane.getItems().addAll(editorPreviewSplit, logSplitPane); // logBox = VBox with logLabel + logArea
        rootSplitPane.setDividerPositions(0.8);

        // --- Root BorderPane ---
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(rootSplitPane);
        scene = new Scene(root, 1600, 900);
        primaryStage.setScene(scene);
        primaryStage.show();
        ThemeManager.register(this);
        Workspace.createWorkspaceRootFolder();

        // Apply styles
        scene.getStylesheets().add(UiUtil.getStylesFromStyleSheet(ideStyleSheet));
        errorReporter.reportError("Welcome to Campaign workbench!", false);
    }

    /**
     * Stop the application
     */
    @Override
    public void stop() {
        exitApplication();
    }

    private void exitApplication() {
        workspaceExplorer.saveWorkspace();
        editorTabPanel.closeAllTabs();
        Platform.exit();
        System.exit(0);
    }

    private void setThemeHandler(IdeTheme ideTheme) {
        ThemeManager.setTheme(ideTheme);
    }

    /**
     * Applies a theme to the IDE
     *
     */
    @Override
    public void applyTheme(IdeTheme oldTheme, IdeTheme newTheme) {

        scene.getStylesheets().remove(oldTheme.getIdeStyleSheet());

        // Set AtlantaFX styles
        Application.setUserAgentStylesheet(null); // clear first
        Application.setUserAgentStylesheet(newTheme.getAtlantaFxStyleSheet());

        // Set IDE theme styles
        scene.getStylesheets().add(newTheme.getIdeStyleSheet());
    }

    /**
     * Updates the run button state based on the current tab
     *
     * @param tab the currently selected tab
     */
    private void tabPanelChanged(Tab tab) {
        if (tab instanceof EditorTab editorTab) {
            toolBar.setRunButtonState(editorTab.isTemplateTab());
            currentEditorTab = editorTab;
        }
    }

    /**
     * Opens a workspace directory
     */
    private void openWorkspaceHandler() {
        try {
            workspaceExplorer.openWorkspace();
        } catch (IdeException ideEx) {
            errorReporter.reportError("An error occurred while opening the workspace: " + ideEx.getMessage(), ideEx, true);
        }
    }

    private void newWorkspaceHandler() {
        try {
            workspaceExplorer.createNewWorkspace();
        } catch (IdeException ideEx) {
            errorReporter.reportError("An error occurred while creating a new workspace: " + ideEx.getMessage(), ideEx, true);
        }
    }

    private void saveWorkspaceHandler() {
        try {
            workspaceExplorer.saveWorkspace();
        } catch (IdeException ideEx) {
            errorReporter.reportError("An error occurred while saving the workspace: " + ideEx.getMessage(), ideEx, true);
        }
    }

    private void closeWorkspaceHandler() {
        try {
            editorTabPanel.closeAllTabs();
            workspaceExplorer.closeWorkspace();
        } catch (IdeException ideEx) {
            errorReporter.reportError("An error occurred while closing the workspace: " + ideEx.getMessage(), ideEx, true);
        }
    }

    private void closeEditorTabsHandler() {
        editorTabPanel.closeAllTabs();
    }

    private void saveWorkspaceAs() {
        try {
            workspaceExplorer.saveWorkspace();
        } catch (IdeException ideEx) {
            errorReporter.reportError("An error occurred while saving the workspace: " + ideEx.getMessage(), ideEx, true);
        }
    }

    /**
     * Opens a file from the workspace explorer
     *
     * @param workspaceFile the file to open
     */
    private void openFileFromWorkspace(WorkspaceFile workspaceFile) {
        openFileInNewTab(workspaceFile);
    }

    private void workspaceChanged(Workspace newWorkspace) {
        editorTabPanel.closeAllTabs();
    }

    private void insertIntoCodeHandler(String code) {
        System.out.println("Insert into code: " + code);
        editorTabPanel.insertTextIntoSelected(code);
    }

    /**
     * Opens a file from the file system
     *
     */
    private void addExistingFileHandler(WorkspaceFileType fileType) {
        try {
            workspaceExplorer.addExistingFile(fileType);
        } catch (IdeException ideEx) {
            errorReporter.reportError("An error occurred while adding an existing file of type: " + fileType, ideEx, true);
        }
    }

    private void createNewFileHandler(WorkspaceFileType fileType) {
        try {
            workspaceExplorer.createNewFile(fileType);
            saveWorkspaceHandler();
        } catch (IdeException ideEx) {
            errorReporter.reportError("An error occurred while creating an new file of type: " + fileType, ideEx, true);
        }
    }

    /**
     * Opens a file in a new editor tab
     *
     * @param workspaceFile the file to open
     */
    private void openFileInNewTab(WorkspaceFile workspaceFile) {
        editorTabPanel.addEditorTab(workspaceFile);
    }

    /**
     * Saves the content of the currently selected editor tab
     */
    private void saveCurrentFileHandler() {

        if (!editorTabPanel.isSelected()) {
            errorReporter.reportError("No editor tab selected to save.", true);
            return;
        }
        editorTabPanel.saveSelectedTab();
    }

    private void saveCurrentFileAsHandler() {

    }

    private void toggleFindHandler() {
        if (currentEditorTab != null) {
            currentEditorTab.toggleFind();
        }
    }

    /**
     * Runs the template in the currently selected editor tab
     */
    private void runTemplate() {
        if (!workspaceExplorer.isWorkspaceOpen()) {
            errorReporter.reportError("No workspace is open. Please open a workspace before running a template.", true);
            return;
        }

        errorLogPanel.clearErrors();
        saveWorkspaceHandler();
        try {
            WorkspaceFile selectedWorkspaceFile = editorTabPanel.getSelectedWorkspaceFile();

            if (selectedWorkspaceFile instanceof Template workspaceContextFile) {

                TemplateRenderResult renderResult = templateRenderer.render(
                        workspaceExplorer.getWorkspace(),
                        workspaceContextFile
                );

                String resultHtml = renderResult.renderedOutput();
                String resultJs = renderResult.generatedJavaScript();

                Platform.runLater(() -> {
                    outputPanel.setContent(resultHtml, resultJs);
                    errorReporter.logMessage("Template ran successfully: " + editorTabPanel.getSelectedFileName());
                });
            }
        } catch (IdeException ideEx) {
            errorReporter.reportError("An IDE error occurred!", ideEx, true);
        } catch (RendererException renderEx) {
            outputPanel.setContent("<html><head><title>Error!</title><body><strong>Error rendering HTML</strong></body></html>", renderEx.getSourceCode());
            errorReporter.reportError("A Renderer error occurred!", renderEx, true);
        } catch (Exception ex) {
            errorReporter.reportError("An unexpected error occurred!", ex, true);
        }
    }

    /**
     * Shows an alert dialog with the specified message
     *
     * @param msg the message to show
     */
    private static void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(
                    Alert.AlertType.WARNING,
                    msg,
                    ButtonType.OK
            );
            alert.showAndWait();
        });
    }

    private void showAbout() {
        AboutDialog.show(scene.getWindow());
    }

    private void showSettings() {
        SettingsDialog.show((Stage) scene.getWindow(), appSettings);
    }
}
