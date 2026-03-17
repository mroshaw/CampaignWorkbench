package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.ApiException;
import com.campaignworkbench.adobecampaignapi.CampaignInstance;
import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.*;
import com.campaignworkbench.ide.AppSettings;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.ide.dialogs.YesNoPopupDialog;

import com.campaignworkbench.workspace.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CampaignOperationsHandler {

    private final CampaignServerManager campaignServerManager;
    private final ErrorReporter errorReporter;
    private Consumer<WorkspaceFile> fileOpenHandler;
    private Supplier<Scene> sceneSupplier;

    private final AppSettings appSettings;

    private Workspace workspace;

    public CampaignOperationsHandler(
            ErrorReporter errorReporter,
            AppSettings appSettings) {

        this.errorReporter = errorReporter;
        this.appSettings = appSettings;
        this.campaignServerManager = new CampaignServerManager();
    }

    public void setFileOpenHandler(Consumer<WorkspaceFile> fileOpenHandler) {
        this.fileOpenHandler = fileOpenHandler;
    }

    public void setSceneSupplier(Supplier<Scene> sceneSupplier) {
        this.sceneSupplier = sceneSupplier;
    }

    /**
     * Called when a workspace is opened or created. Looks up the CampaignInstance
     * for the workspace and wires it into the CampaignServerManager.
     */
    public void setWorkspace(Workspace workspace) {
        if(campaignServerManager.getConnectedObservable().get()) {
            disconnectFromCampaign();
        }

        this.workspace = workspace;
        if (workspace == null || workspace.getCampaignInstanceId() == null) {
            campaignServerManager.setCampaignInstance(null);
            return;
        }

        Optional<CampaignInstance> instance = appSettings.findById(workspace.getCampaignInstanceId());
        if (instance.isPresent()) {
            campaignServerManager.setCampaignInstance(instance.get());
        } else {
            campaignServerManager.setCampaignInstance(null);
            errorReporter.reportError(
                    "The Campaign instance configured for this workspace could not be found. " +
                            "Please check File > Settings and reassign an instance.", true);
        }
    }

    public SimpleBooleanProperty getConnectedObservable() {
        return campaignServerManager.getConnectedObservable();
    }

    public String connectToCampaign() {
        try {
            withBusyCursor(() -> {
                campaignServerManager.connect();
            });

        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred connecting to Adobe Campaign. Please check File > Settings!", apiException, true);
            return "";
        }
        errorReporter.logMessage("Connected to Campaign server at: " + campaignServerManager.getEndpointUrl());
        String endPointUrl = campaignServerManager.getEndpointUrl();
        String host = URI.create(endPointUrl).getHost();
        String hostname = host.split("\\.")[0];
        return hostname;
    }

    public Boolean disconnectFromCampaign() {
        try {
            withBusyCursor(() -> {
                campaignServerManager.disconnect();
            });
            errorReporter.logMessage("Disconnected from Campaign at: " + campaignServerManager.getEndpointUrl());
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred disconnecting from Adobe Campaign", apiException, true);
        }
        return true;
    }

    public void createNewFile(WorkspaceFileType workspaceFileType) {
        if(workspaceFileType == WorkspaceFileType.BLOCK) {
            createNewBlock();
        }
        if(workspaceFileType == WorkspaceFileType.MODULE)
        {
            createNewModule();
        }
    }

    public void createNewBlock() {
        Optional<PersoBlockRecord> newBlock = CampaignBlockPickerDialog.show(sceneSupplier.get().getWindow(), campaignServerManager);
        if (newBlock.isPresent()) {
            PersoBlockRecord persoBlockRecord = newBlock.get();
            PersoBlockSchemaKey key = (PersoBlockSchemaKey) persoBlockRecord.getKey();
            try {
                withBusyCursor(() -> {
                    WorkspaceFile newFile = workspace.createNewWorkspaceFile(persoBlockRecord.getName(), WorkspaceFileType.BLOCK, persoBlockRecord.getCode(), key);
                    newFile.setKey(key);
                    Platform.runLater(() -> fileOpenHandler.accept(newFile));
                });

            } catch (WorkspaceException we) {
                errorReporter.reportError("An error occurred while creating " + persoBlockRecord.getName(), we, true);
            }
        }
    }

    public void createNewModule() {
        Optional<EtmModuleRecord> newModule = CampaignModulePickerDialog.show(sceneSupplier.get().getWindow(), campaignServerManager);
        if (newModule.isPresent()) {
            EtmModuleRecord etmModuleRecord = newModule.get();
            EtmModuleSchemaKey key = (EtmModuleSchemaKey) etmModuleRecord.getKey();
            try {
                withBusyCursor(() -> {
                    WorkspaceFile newFile = workspace.createNewWorkspaceFile(etmModuleRecord.getName(), WorkspaceFileType.MODULE, etmModuleRecord.getCode(), key);
                    newFile.setKey(key);
                    Platform.runLater(() -> fileOpenHandler.accept(newFile));
                });

            } catch (WorkspaceException we) {
                errorReporter.reportError("An error occurred while creating " + etmModuleRecord.getName(), we, true);
            }
        }
    }

    public void refresh(WorkspaceFile workspaceFile) {
        WorkspaceFileType fileType = workspaceFile.getFileType();

        if (!workspaceFile.hasCampaignKey()) {
            errorReporter.reportError("The selected file does not have a Campaign key!", true);
            return;
        }

        if (YesNoPopupDialog.show("Are you sure?", "Are you sure you want to refresh " + fileType + " " + workspaceFile.getFileName() + " from Adobe Campaign?", (Stage) sceneSupplier.get().getWindow()) != YesNoPopupDialog.YesNo.YES) {
            return;
        }
        errorReporter.logMessage("Refreshing " + workspaceFile.getBaseFileName() + " from Adobe Campaign. Please wait...");
        try {
            if (fileType == WorkspaceFileType.BLOCK) {
                withBusyCursor(() -> {
                    campaignServerManager.refreshBlocks();
                    PersoBlockSchemaKey key = (PersoBlockSchemaKey) workspaceFile.getKey();
                    Optional<PersoBlockRecord> block = campaignServerManager.getPersonalizationBlock(key);

                    if (block.isPresent()) {
                        workspaceFile.saveWorkspaceFileContent(block.get().getCode());
                        Platform.runLater(() -> fileOpenHandler.accept(workspaceFile));
                    }
                });
            }

            if (fileType == WorkspaceFileType.MODULE) {
                campaignServerManager.refreshJavaScriptTemplates();
                EtmModuleSchemaKey key = (EtmModuleSchemaKey) workspaceFile.getKey();
                Optional<EtmModuleRecord> javascriptTemplate = campaignServerManager.getJavaScriptTemplate(key);
                if (javascriptTemplate.isPresent()) {
                    workspaceFile.saveWorkspaceFileContent(javascriptTemplate.get().getCode());
                    Platform.runLater(() -> fileOpenHandler.accept(workspaceFile));
                }
            }
            errorReporter.logMessage("Refreshed " + fileType + " " + workspaceFile.getBaseFileName() + " from Adobe Campaign");
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred refreshing " + workspaceFile.getBaseFileName() + " from Adobe Campaign", apiException, true);
        }
    }

    public void push(WorkspaceFile workspaceFile) {
        WorkspaceFileType fileType = workspaceFile.getFileType();

        if (!workspaceFile.hasCampaignKey()) {
            errorReporter.reportError("The selected file does not have a Campaign key!", true);
        }

        String fileTypeLower = fileType.toString().toLowerCase();
        String fileName = workspaceFile.getFileName();

        if (YesNoPopupDialog.show("Are you sure?", "Are you sure you want to push " + fileTypeLower + " " + fileName + " to Adobe Campaign?", (Stage) sceneSupplier.get().getWindow()) == YesNoPopupDialog.YesNo.NO) {
            return;
        }

        boolean doBackup = YesNoPopupDialog.show("Backup existing code?", "Do you want to create a backup of " + fileTypeLower + " " + fileName + " from Adobe Campaign?", (Stage) sceneSupplier.get().getWindow()) == YesNoPopupDialog.YesNo.YES;

        if (workspaceFile instanceof EtmModule etmModule) {
            EtmModuleSchemaKey moduleKey = etmModule.getKey();
            if (doBackup) {
                Optional<EtmModuleRecord> backupModule = campaignServerManager.getJavaScriptTemplate(moduleKey);
                if (backupModule.isPresent()) {
                    try {
                        workspace.createBackup(workspaceFile, backupModule.get().getCode());
                        errorReporter.logMessage("Successfully backed up " + fileTypeLower + " " + fileName + " from Campaign server!");
                    } catch (WorkspaceException we) {
                        errorReporter.reportError("An error occurred while creating the backup of " + fileTypeLower + " " + fileName, we, true);
                    }
                }
            }
            try {
                campaignServerManager.updateJavascriptTemplate(moduleKey, workspaceFile.getWorkspaceFileContent());
                errorReporter.logMessage("Successfully updated " + fileTypeLower + " " + fileName + " on Campaign server!");
            } catch (ApiException apiException) {
                errorReporter.reportError("Failed to update " + fileTypeLower + " " + fileName + " on Campaign server!", apiException, true);
            }
        } else if (workspaceFile instanceof PersoBlock persoBlock) {
            PersoBlockSchemaKey blockKey = persoBlock.getKey();
            if (doBackup) {
                Optional<PersoBlockRecord> backupBlock = campaignServerManager.getPersonalizationBlock(blockKey);
                if (backupBlock.isPresent()) {
                    try {
                        workspace.createBackup(workspaceFile, backupBlock.get().getCode());
                        errorReporter.logMessage("Successfully backed up " + fileTypeLower + " " + fileName + " from Campaign server!");
                    } catch (WorkspaceException we) {
                        errorReporter.reportError("An error occurred while creating the backup of " + fileTypeLower + " " + fileName, we, true);
                    }
                }
            }
            try {
                campaignServerManager.updatePersonalizationBlock(blockKey, workspaceFile.getWorkspaceFileContent());
                errorReporter.logMessage("Successfully updated " + fileTypeLower + " " + fileName + " on Campaign server!");
            } catch (ApiException apiException) {
                errorReporter.reportError("Failed to update " + fileTypeLower + " " + fileName + " on Campaign server!", apiException, true);
            }
        } else {
            errorReporter.reportError("File " + fileTypeLower + " " + fileName + " is not supported for syncing with Campaign server!", true);
        }
    }

    private void withBusyCursor(Runnable operation) {
        Scene scene = sceneSupplier.get();
        scene.setCursor(Cursor.WAIT);
        Thread.ofVirtual().start(() -> {
            try {
                operation.run();
            } finally {
                Platform.runLater(() -> scene.setCursor(Cursor.DEFAULT));
            }
        });
    }

}