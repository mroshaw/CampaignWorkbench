package com.campaignworkbench.ide;

import com.campaignworkbench.adobecampaignapi.CampaignConnectionManager;
import com.campaignworkbench.adobecampaignapi.ConnectedStatus;
import com.campaignworkbench.campaignrenderer.RendererException;
import com.campaignworkbench.campaignrenderer.TemplateRenderResult;
import com.campaignworkbench.campaignrenderer.TemplateRenderer;
import com.campaignworkbench.ide.dialogs.AboutDialog;
import com.campaignworkbench.ide.dialogs.SettingsDialog;
import com.campaignworkbench.ide.editor.EditorTabPanel;
import com.campaignworkbench.ide.logging.ErrorLogPanel;
import com.campaignworkbench.ide.logging.LogPanel;
import com.campaignworkbench.ide.logging.UiErrorReporter;
import com.campaignworkbench.ide.results.OutputTabPanel;
import com.campaignworkbench.ide.themes.IThemeable;
import com.campaignworkbench.ide.themes.IdeTheme;
import com.campaignworkbench.ide.themes.ThemeManager;
import com.campaignworkbench.ide.toolbars.ConnectionToolBar;
import com.campaignworkbench.ide.toolbars.MainMenuBar;
import com.campaignworkbench.ide.toolbars.RunToolBar;
import com.campaignworkbench.ide.toolbars.WorkspaceToolBar;
import com.campaignworkbench.adobecampaignapi.CampaignOperationsHandler;
import com.campaignworkbench.ide.workspaceexplorer.WorkspaceExplorer;
import com.campaignworkbench.util.UiUtil;
import com.campaignworkbench.workspace.Template;
import com.campaignworkbench.workspace.Workspace;
import com.campaignworkbench.workspace.WorkspaceFileType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Builds a User Interface for the Campaign Workbench IDE
 */
public class CampaignWorkbenchIDE extends Application implements IThemeable {
    private static final String userHomeFolderName = "Campaign Workbench";
    public static final Path userHome =  Paths.get(System.getProperty("user.home")).resolve(userHomeFolderName);
    private static final String ideStyleSheet = "/styles/ide_general_styles.css";

    private WorkspaceExplorer workspaceExplorer;
    private RunToolBar runToolBar;
    private EditorTabPanel editorTabPanel;
    private UiErrorReporter errorReporter;
    private ErrorLogPanel errorLogPanel;
    private OutputTabPanel outputPanel;
    private Scene scene;
    private AppSettings appSettings;
    private CampaignConnectionManager campaignConnectionManager;
    private CampaignOperationsHandler campaignOperationsHandler;
    private final TemplateRenderer templateRenderer = new TemplateRenderer();

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
        errorLogPanel.setOnErrorDoubleClicked((_, line) -> outputPanel.highlightJsLine(line));
        errorReporter = new UiErrorReporter(logPanel, errorLogPanel);

        // Create the user home, if not already created
        try {
            checkUserHome();
        } catch (IdeException e) {
            errorReporter.reportError("A FATAL error occurred creating the user home folder! Cannot proceed!", true);
        }

        try {
            AppSettings.checkAppSettings();
        }  catch (IdeException e) {
            errorReporter.reportError("A FATAL error occurred creating the app settings folder! Cannot proceed!", true);
        }

        try {
            Workspace.checkWorkspaceRoot();
        } catch (IdeException e) {
            errorReporter.reportError("A FATAL error occurred creating the workspace folder! Cannot proceed!", true);
        }

        appSettings = AppSettingsManager.load();

        // Create a Campaign Operations Handler
        campaignOperationsHandler = new CampaignOperationsHandler(errorReporter, appSettings);
        campaignOperationsHandler.getConnectedObservable().addListener((_, _, newValue) -> connectedStatusChangedHandler(newValue));

        // Editor tab panel
        editorTabPanel = new EditorTabPanel(null, errorReporter, campaignOperationsHandler.getConnectedObservable(),
                campaignOperationsHandler::refresh, campaignOperationsHandler::push, campaignOperationsHandler::createOnServer);

        // Workspace Explorer
        workspaceExplorer = new WorkspaceExplorer("Workspace Explorer", editorTabPanel::addEditorTab,
                campaignOperationsHandler.getConnectedObservable(), editorTabPanel::insertTextIntoSelected, campaignOperationsHandler::createNewFile, errorReporter, appSettings);

        campaignOperationsHandler.setFileOpenHandler(editorTabPanel::addEditorTab);

        // Reset the connection if the workspace changes
        workspaceExplorer.getWorkspaceObservable().addListener((_, _, newW) -> campaignOperationsHandler.setWorkspace(newW));
        ConnectionToolBar campaignConnectionToolBar = new ConnectionToolBar(campaignOperationsHandler::connectToCampaign,
                campaignOperationsHandler::disconnectFromCampaign,
                campaignOperationsHandler.getConnectedObservable(),
                workspaceExplorer.getWorkspaceIsSetObservable());

        // Run Toolbar
        runToolBar = new RunToolBar(this::runTemplate);

        // Menu and toolbar
        MainMenuBar menuBar = new MainMenuBar(
                _ -> workspaceExplorer.createNewWorkspace(),
                _ -> workspaceExplorer.openWorkspace(),
                _ -> workspaceExplorer.saveWorkspace(),
                _ -> workspaceExplorer.closeWorkspace(),
                _ -> workspaceExplorer.createNewFile(WorkspaceFileType.TEMPLATE),
                _ -> workspaceExplorer.createNewFile(WorkspaceFileType.MODULE),
                _ -> workspaceExplorer.createNewFile(WorkspaceFileType.BLOCK),
                _ -> workspaceExplorer.createNewFile(WorkspaceFileType.CONTEXT),

                _ -> workspaceExplorer.addExistingFile(WorkspaceFileType.TEMPLATE),
                _ -> workspaceExplorer.addExistingFile(WorkspaceFileType.MODULE),
                _ -> workspaceExplorer.addExistingFile(WorkspaceFileType.BLOCK),
                _ -> workspaceExplorer.addExistingFile(WorkspaceFileType.CONTEXT),

                _ -> editorTabPanel.saveSelectedTab(),
                _ -> editorTabPanel.saveSelectedTabAs(),

                _ -> showSettings(),

                _ -> setThemeHandler(IdeTheme.LIGHT),
                _ -> setThemeHandler(IdeTheme.DARK),

                _ -> showAbout(),
                _ -> exitApplication()
        );


        // Refresh templates if the workspace changes
        workspaceExplorer.getWorkspaceObservable().addListener((_, _, newW) -> {
            if(newW != null) {
                runToolBar.setTemplateObservableList(newW.getTemplates());
            }
        });

        // Create the workspace toolbar
        WorkspaceToolBar workspaceToolBar = new WorkspaceToolBar(workspaceExplorer::openWorkspace, workspaceExplorer::createNewWorkspace, workspaceExplorer::closeWorkspace);

        // Close all tabs when workspace changes
        workspaceExplorer.getWorkspaceObservable().addListener((_, _, _) -> editorTabPanel.closeAllTabs());

        // Output panes
        outputPanel = new OutputTabPanel();

        SplitPane logSplitPane = new SplitPane();
        logSplitPane.setOrientation(Orientation.HORIZONTAL);
        logSplitPane.getItems().addAll(logPanel.getNode(), errorLogPanel.getNode());
        logSplitPane.setDividerPositions(0.5);

        // Workspace Explorer | Editor Tabs
        SplitPane workspaceEditorSplit = new SplitPane();
        workspaceEditorSplit.setOrientation(Orientation.HORIZONTAL);
        workspaceEditorSplit.getItems().addAll(
                workspaceExplorer.getNode(),
                editorTabPanel.getNode()
        );
        workspaceEditorSplit.setDividerPositions(0.30);
        SplitPane.setResizableWithParent(workspaceExplorer.getNode(), false);

        // Workspace+Editor | Preview
        SplitPane editorPreviewSplit = new SplitPane();
        editorPreviewSplit.setOrientation(Orientation.HORIZONTAL);
        editorPreviewSplit.getItems().addAll(
                workspaceEditorSplit,
                outputPanel.getNode()
        );
        editorPreviewSplit.setDividerPositions(0.71);

        ToolBar ideToolBar = new ToolBar(workspaceToolBar.getNode(), new Separator(Orientation.VERTICAL), campaignConnectionToolBar.getNode(), new Separator(Orientation.VERTICAL), runToolBar.getNode());
        ideToolBar.getStyleClass().add("large-toolbar");

        VBox topBar = new VBox(menuBar.getNode(), ideToolBar);

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

        campaignOperationsHandler.setSceneSupplier(this::getScene);

        // Apply styles
        scene.getStylesheets().add(UiUtil.getStylesFromStyleSheet(ideStyleSheet));
        errorReporter.reportError("Welcome to Campaign workbench!", false);
    }

    private Scene getScene() {
        return scene;
    }

    private void connectedStatusChangedHandler(ConnectedStatus connectedStatus) {
        if(connectedStatus.getIsConnected()) {
            errorReporter.logMessage("Connected to: " + connectedStatus.getConnectionName());
        } else {
            errorReporter.logMessage("Disconnected from campaign.");
        }
    }

    private void checkUserHome() {
        if (!Files.exists(userHome)) {
            try {
                System.out.println("Creating user home: " + userHome);
                Files.createDirectory(userHome);
                System.out.println("Created user home: " + userHome);
            } catch (IOException ioe) {
                errorReporter.reportError("An error occurred creating user home: " + userHome, ioe, true);
            }
        }
    }

    // Stop the application
    @Override
    public void stop() {
        exitApplication();
    }

    private void exitApplication() {
        workspaceExplorer.saveWorkspace();
        editorTabPanel.closeAllTabs();
        campaignOperationsHandler.disconnectFromCampaign();
        Platform.exit();
        System.exit(0);
    }

    private void setThemeHandler(IdeTheme ideTheme) {
        ThemeManager.setTheme(ideTheme);
    }

    // Apply theme style sheets and update AtlantaFX
    @Override
    public void applyTheme(IdeTheme oldTheme, IdeTheme newTheme) {

        scene.getStylesheets().remove(oldTheme.getIdeStyleSheet());

        // Set AtlantaFX styles
        Application.setUserAgentStylesheet(null); // clear first
        Application.setUserAgentStylesheet(newTheme.getAtlantaFxStyleSheet());

        // Set IDE theme styles
        scene.getStylesheets().add(newTheme.getIdeStyleSheet());
    }

    // Renders the selected template
    private void runTemplate(Template selectedTemplate) {
        if (!workspaceExplorer.isWorkspaceOpen()) {
            errorReporter.reportError("No workspace is open. Please open a workspace before running a template.", true);
            return;
        }

        errorLogPanel.clearErrors();
        try {
            TemplateRenderResult renderResult = templateRenderer.render(
                    workspaceExplorer.getWorkspace(),
                    selectedTemplate
            );

            String resultHtml = renderResult.renderedOutput();
            String resultJs = renderResult.generatedJavaScript();

            Platform.runLater(() -> {
                outputPanel.setContent(resultHtml, resultJs);
                errorReporter.logMessage("Template ran successfully: " + selectedTemplate.getFileName());
            });

        } catch (IdeException ideEx) {
            errorReporter.reportError("An IDE error occurred!", ideEx, true);
        } catch (RendererException renderEx) {
            outputPanel.setContent("<html><head><title>Error!</title><body><strong>Error rendering HTML</strong></body></html>", renderEx.getSourceCode());
            errorReporter.reportError("A Renderer error occurred!", renderEx, true);
        } catch (Exception ex) {
            errorReporter.reportError("An unexpected error occurred!", ex, true);
        }
    }

    private void showAbout() {
        AboutDialog.show(scene.getWindow());
    }

    private void showSettings() {
        SettingsDialog.show(scene.getWindow(), appSettings, errorReporter);
    }
}
