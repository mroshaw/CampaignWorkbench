package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.ApiException;
import com.campaignworkbench.adobecampaignapi.CampaignInstance;
import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.EtmModuleSchemaKey;
import com.campaignworkbench.adobecampaignapi.schemas.EtmModuleRecord;
import com.campaignworkbench.adobecampaignapi.schemas.PersoBlockRecord;
import com.campaignworkbench.adobecampaignapi.schemas.PersoBlockSchemaKey;
import com.campaignworkbench.ide.AppSettings;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.ide.dialogs.YesNoPopupDialog;

import com.campaignworkbench.workspace.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CampaignOperationsHandler {

    private final CampaignServerManager campaignServerManager;
    private final ErrorReporter errorReporter;
    private final Consumer<WorkspaceFile> fileOpenHandler;
    private final Supplier<Workspace> workspaceSupplier;
    private final Supplier<WorkspaceFile> selectedFileSupplier;
    private final Supplier<WorkspaceFileType> selectedFileTypeSupplier;
    private final Runnable onConnectionStateChanged;
    private final Supplier<Window> windowSupplier;
    private final AppSettings appSettings;

    private boolean isConnectedToCampaign;
    private String connectionHostLabel = "";

    public CampaignOperationsHandler(
            ErrorReporter errorReporter,
            Consumer<WorkspaceFile> fileOpenHandler,
            Supplier<Workspace> workspaceSupplier,
            Supplier<WorkspaceFile> selectedFileSupplier,
            Supplier<WorkspaceFileType> selectedFileTypeSupplier,
            Runnable onConnectionStateChanged,
            Supplier<Window> windowSupplier,
            AppSettings appSettings) {

        this.errorReporter = errorReporter;
        this.fileOpenHandler = fileOpenHandler;
        this.workspaceSupplier = workspaceSupplier;
        this.selectedFileSupplier = selectedFileSupplier;
        this.selectedFileTypeSupplier = selectedFileTypeSupplier;
        this.onConnectionStateChanged = onConnectionStateChanged;
        this.windowSupplier = windowSupplier;
        this.appSettings = appSettings;
        this.campaignServerManager = new CampaignServerManager();
    }

    /**
     * Called when a workspace is opened or created. Looks up the CampaignInstance
     * for the workspace and wires it into the CampaignServerManager.
     */
    public void onWorkspaceChanged(Workspace workspace) {
        isConnectedToCampaign = false;
        connectionHostLabel = "";

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

    public boolean isConnectedToCampaign() {
        return isConnectedToCampaign;
    }

    public String getConnectionHostLabel() {
        return connectionHostLabel;
    }

    public void connectToCampaignHandler() {
        try {
            campaignServerManager.connect();
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred connecting to Adobe Campaign. Please check File > Settings!", apiException, true);
            return;
        }
        errorReporter.logMessage("Connected to Campaign server at: " + campaignServerManager.getEndpointUrl());
        isConnectedToCampaign = true;
        String endPointUrl = campaignServerManager.getEndpointUrl();
        String host = URI.create(endPointUrl).getHost();
        String hostname = host.split("\\.")[0];
        connectionHostLabel = hostname;
        Platform.runLater(onConnectionStateChanged::run);
    }

    public void disconnectFromCampaignHandler() {
        try {
            campaignServerManager.disconnect();
            errorReporter.logMessage("Disconnected from Campaign at: " + campaignServerManager.getEndpointUrl());
            isConnectedToCampaign = false;
            connectionHostLabel = "";
            Platform.runLater(onConnectionStateChanged::run);
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred disconnecting from Adobe Campaign", apiException, true);
        }
    }

    public Optional<Object> confirmCreateNew() {
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();

        if (selectedFileType == WorkspaceFileType.BLOCK) {
            Optional<PersoBlockRecord> newBlock = CampaignBlockPickerDialog.show(windowSupplier.get(), campaignServerManager);
            return newBlock.map(b -> (Object) b);
        }

        if (selectedFileType == WorkspaceFileType.MODULE) {
            Optional<EtmModuleRecord> newModule = CampaignModulePickerDialog.show(windowSupplier.get(), campaignServerManager);
            return newModule.map(m -> (Object) m);
        }

        return Optional.empty();
    }

    public void executeCreateNew(Object record) {
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();

        if (record instanceof PersoBlockRecord newBlock) {
            PersoBlockSchemaKey key = (PersoBlockSchemaKey) newBlock.getKey();
            try {
                WorkspaceFile newFile = workspaceSupplier.get().createNewWorkspaceFile(newBlock.getName(), WorkspaceFileType.BLOCK, newBlock.getCode(), key);
                newFile.setKey(key);
                Platform.runLater(() -> fileOpenHandler.accept(newFile));
            } catch (WorkspaceException we) {
                errorReporter.reportError("An error occurred while creating " + newBlock.getName(), we, true);
            }

        }

        if (record instanceof EtmModuleRecord newModule) {
            EtmModuleSchemaKey key = (EtmModuleSchemaKey) newModule.getKey();
            try {
                WorkspaceFile newFile = workspaceSupplier.get().createNewWorkspaceFile(newModule.getName(), WorkspaceFileType.MODULE, newModule.getCode(), key);
                newFile.setKey(key);
                Platform.runLater(() -> fileOpenHandler.accept(newFile));
            } catch (WorkspaceException we) {
                errorReporter.reportError("An error occurred while creating " + newModule.getName(), we, true);
            }
        }

        errorReporter.logMessage("Created new " + selectedFileType + " from Adobe Campaign");
    }

    public boolean confirmRefresh() {
        WorkspaceFile selectedFile = selectedFileSupplier.get();
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();

        if (!selectedFile.hasCampaignKey()) {
            errorReporter.reportError("The selected file does not have a Campaign key!", true);
            return false;
        }

        return YesNoPopupDialog.show("Are you sure?", "Are you sure you want to refresh " + selectedFileType + " " + selectedFile.getFileName() + " from Adobe Campaign?", (Stage) windowSupplier.get()) == YesNoPopupDialog.YesNo.YES;
    }

    public void executeRefresh() {
        WorkspaceFile selectedFile = selectedFileSupplier.get();
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();

        errorReporter.logMessage("Refreshing " + selectedFile.getBaseFileName() + " from Adobe Campaign. Please wait...");
        try {
            if (selectedFileType == WorkspaceFileType.BLOCK) {
                campaignServerManager.refreshBlocks();
                PersoBlockSchemaKey key = (PersoBlockSchemaKey) selectedFile.getKey();
                Optional<PersoBlockRecord> block = campaignServerManager.getPersonalizationBlock(key);
                if (block.isPresent()) {
                    selectedFile.saveWorkspaceFileContent(block.get().getCode());
                    Platform.runLater(() -> fileOpenHandler.accept(selectedFile));
                }
            }

            if (selectedFileType == WorkspaceFileType.MODULE) {
                campaignServerManager.refreshJavaScriptTemplates();
                EtmModuleSchemaKey key = (EtmModuleSchemaKey) selectedFile.getKey();
                Optional<EtmModuleRecord> javascriptTemplate = campaignServerManager.getJavaScriptTemplate(key);
                if (javascriptTemplate.isPresent()) {
                    selectedFile.saveWorkspaceFileContent(javascriptTemplate.get().getCode());
                    Platform.runLater(() -> fileOpenHandler.accept(selectedFile));
                }
            }
            errorReporter.logMessage("Refreshed " + selectedFileType + " " + selectedFile.getBaseFileName() + " from Adobe Campaign");
        } catch (ApiException apiException) {
            errorReporter.reportError("An error occurred refreshing " + selectedFile.getBaseFileName() + " from Adobe Campaign", apiException, true);
        }
    }

    public Optional<Boolean> confirmPush() {
        WorkspaceFile selectedFile = selectedFileSupplier.get();
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();

        if (!selectedFile.hasCampaignKey()) {
            errorReporter.reportError("The selected file does not have a Campaign key!", true);
            return Optional.empty();
        }

        String fileTypeLower = selectedFileType.toString().toLowerCase();
        String selectedFileName = selectedFile.getFileName();

        if (YesNoPopupDialog.show("Are you sure?", "Are you sure you want to push " + fileTypeLower + " " + selectedFileName + " to Adobe Campaign?", (Stage) windowSupplier.get()) == YesNoPopupDialog.YesNo.NO) {
            return Optional.empty();
        }

        boolean doBackup = YesNoPopupDialog.show("Backup existing code?", "Do you want to create a backup of " + fileTypeLower + " " + selectedFileName + " from Adobe Campaign?", (Stage) windowSupplier.get()) == YesNoPopupDialog.YesNo.YES;
        return Optional.of(doBackup);
    }

    public void executePush(boolean doBackup) {
        WorkspaceFile selectedFile = selectedFileSupplier.get();
        WorkspaceFileType selectedFileType = selectedFileTypeSupplier.get();
        String fileTypeLower = selectedFileType.toString().toLowerCase();
        String selectedFileName = selectedFile.getFileName();

        if (selectedFile instanceof EtmModule etmModule) {
            EtmModuleSchemaKey moduleKey = etmModule.getKey();
            if (doBackup) {
                Optional<EtmModuleRecord> backupModule = campaignServerManager.getJavaScriptTemplate(moduleKey);
                if (backupModule.isPresent()) {
                    try {
                        workspaceSupplier.get().createBackup(selectedFile, backupModule.get().getCode());
                        errorReporter.logMessage("Successfully backed up " + fileTypeLower + " " + selectedFileName + " from Campaign server!");
                    } catch (WorkspaceException we) {
                        errorReporter.reportError("An error occurred while creating the backup of " + fileTypeLower + " " + selectedFileName, we, true);
                    }
                }
            }
            try {
                campaignServerManager.updateJavascriptTemplate(moduleKey, selectedFile.getWorkspaceFileContent());
                errorReporter.logMessage("Successfully updated " + fileTypeLower + " " + selectedFileName + " on Campaign server!");
            } catch (ApiException apiException) {
                errorReporter.reportError("Failed to update " + fileTypeLower + " " + selectedFileName + " on Campaign server!", apiException, true);
            }
        } else if (selectedFile instanceof PersoBlock persoBlock) {
            PersoBlockSchemaKey blockKey = persoBlock.getKey();
            if (doBackup) {
                Optional<PersoBlockRecord> backupBlock = campaignServerManager.getPersonalizationBlock(blockKey);
                if (backupBlock.isPresent()) {
                    try {
                        workspaceSupplier.get().createBackup(selectedFile, backupBlock.get().getCode());
                        errorReporter.logMessage("Successfully backed up " + fileTypeLower + " " + selectedFileName + " from Campaign server!");
                    } catch (WorkspaceException we) {
                        errorReporter.reportError("An error occurred while creating the backup of " + fileTypeLower + " " + selectedFileName, we, true);
                    }
                }
            }
            try {
                campaignServerManager.updatePersonalizationBlock(blockKey, selectedFile.getWorkspaceFileContent());
                errorReporter.logMessage("Successfully updated " + fileTypeLower + " " + selectedFileName + " on Campaign server!");
            } catch (ApiException apiException) {
                errorReporter.reportError("Failed to update " + fileTypeLower + " " + selectedFileName + " on Campaign server!", apiException, true);
            }
        } else {
            errorReporter.reportError("File " + fileTypeLower + " " + selectedFileName + " is not supported for syncing with Campaign server!", true);
        }
    }
}