package com.campaignworkbench.adobecampaignapi;

import com.campaignworkbench.adobecampaignapi.schemas.EtmModuleRecord;
import com.campaignworkbench.adobecampaignapi.schemas.EtmModuleSchemaKey;
import com.campaignworkbench.adobecampaignapi.schemas.PersoBlockRecord;
import com.campaignworkbench.adobecampaignapi.schemas.PersoBlockSchemaKey;
import com.campaignworkbench.ide.AppSettings;
import com.campaignworkbench.ide.dialogs.*;
import com.campaignworkbench.ide.logging.ErrorReporter;
import com.campaignworkbench.workspace.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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
        if (campaignServerManager.getConnectedStatusObservable().get().getIsConnected()) {
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
                    "The Campaign instance configured for this workspace could not be found. ", true);
        }
    }

    public ObjectProperty<ConnectedStatus> getConnectedObservable() {
        return campaignServerManager.getConnectedStatusObservable();
    }

    public void connectToCampaign() {
        withBusyCursor(() -> {
            try {
                campaignServerManager.connect(KeystorePasswordProvider.getPassword());
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | ApiException e) {
                // First attempt failed — ask the user for a password and try again
                Platform.runLater(() -> {
                    Optional<char[]> passwordOptional = PasswordInputDialog.show(
                            sceneSupplier.get().getWindow(),
                            "Credentials Password",
                            "Enter credentials password",
                            "Please enter the password used to protect the instance credentials");
                    if (passwordOptional.isEmpty()) {
                        errorReporter.reportError("Cannot connect to campaign instance without a credential store password!", true);
                        return;
                    }
                    withBusyCursor(() -> {
                        try {
                            campaignServerManager.connect(passwordOptional.get());
                        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | ApiException ex) {
                            errorReporter.reportError("An error occurred while trying to connect to the campaign instance.", ex, true);
                        }
                    });
                });
            }
        });
    }

    public void disconnectFromCampaign() {

        withBusyCursor(() -> {
            try {
                campaignServerManager.disconnect();
            } catch (ApiException ex) {
                errorReporter.reportError("An error occurred while trying to connect to the campaign instance.", ex, true);
            }
        });
    }

    public void createNewFile(WorkspaceFileType workspaceFileType) {
        if (workspaceFileType == WorkspaceFileType.BLOCK) {
            createNewBlock();
        }
        if (workspaceFileType == WorkspaceFileType.MODULE) {
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

        if (YesNoPopupDialog.show(sceneSupplier.get().getWindow(), "Are you sure?", "Are you sure you want to refresh " + fileType + " " + workspaceFile.getFileName() + " from Adobe Campaign?") != YesNoPopupDialog.YesNo.YES) {
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

        if (YesNoPopupDialog.show(sceneSupplier.get().getWindow(), "Are you sure?", "Are you sure you want to push " + fileTypeLower + " " + fileName + " to Adobe Campaign?") == YesNoPopupDialog.YesNo.NO) {
            return;
        }

        boolean doBackup = YesNoPopupDialog.show(sceneSupplier.get().getWindow(), "Backup existing code?", "Do you want to create a backup of " + fileTypeLower + " " + fileName + " from Adobe Campaign?") == YesNoPopupDialog.YesNo.YES;

        if (workspaceFile instanceof EtmModule etmModule) {
            EtmModuleSchemaKey moduleKey = etmModule.getKey();
            if (doBackup) {
                campaignServerManager.refreshJavaScriptTemplates();
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
                campaignServerManager.refreshBlocks();
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

    /**
     * Creates a new file on the Campaign server from a locally-created workspace file.
     * Shows a dialog to collect server details, checks for duplicates, creates the record,
     * renames the local file to match the server-canonical name, and sets the key.
     * <p>
     * Only supported for BLOCK and MODULE file types.
     */
    public void createOnServer(WorkspaceFile workspaceFile) {
        WorkspaceFileType fileType = workspaceFile.getFileType();
        Window owner = sceneSupplier.get().getWindow();
        String fileTypeLower = fileType.toString().toLowerCase();

        if (fileType == WorkspaceFileType.BLOCK) {
            createBlockOnServer((PersoBlock) workspaceFile, owner, fileTypeLower);
        } else if (fileType == WorkspaceFileType.MODULE) {
            createModuleOnServer((EtmModule) workspaceFile, owner, fileTypeLower);
        }
    }

    private void createBlockOnServer(PersoBlock block, Window owner, String fileTypeLower) {
        // Loop to allow the user to correct details if a duplicate is found
        while (true) {
            Optional<CreateBlockOnServerDialog.Result> result = CreateBlockOnServerDialog.show(owner, block.getBaseFileName(), campaignServerManager);
            if (result.isEmpty()) {
                return;
            }

            CreateBlockOnServerDialog.Result details = result.get();

            // Check for duplicate before hitting the server
            campaignServerManager.refreshBlocks();
            if (campaignServerManager.personalizationBlockExists(details.name())) {
                errorReporter.reportError(
                        "A personalization block with name '" + details.name() + "' already exists on the server. Please choose a different name.",
                        true);
                continue;
            }

            String canonicalFileName = details.name() + WorkspaceFileType.BLOCK.getFileExtension();

            withBusyCursor(() -> {
                try {
                    PersoBlockRecord created = campaignServerManager.createPersonalizationBlock(
                            details.name(), details.label(),
                            details.folderId(), block.getWorkspaceFileContent());

                    PersoBlockSchemaKey key = (PersoBlockSchemaKey) created.getKey();
                    block.setKey(key);

                    if (!block.getFileName().equals(canonicalFileName)) {
                        workspace.renameWorkspaceFile(block, canonicalFileName);
                    } else {
                        workspace.save();
                    }

                    errorReporter.logMessage("Successfully created " + fileTypeLower + " '" + details.name() + "' on Campaign server! (name=" + details.name() + ", folderId=" + details.folderId() + ")");
                    Platform.runLater(() -> fileOpenHandler.accept(block));

                } catch (ApiException apiException) {
                    errorReporter.reportError("An error occurred creating " + fileTypeLower + " '" + details.name() + "' on the Campaign server", apiException, true);
                }
            });
            return;
        }
    }

    private void createModuleOnServer(EtmModule module, Window owner, String fileTypeLower) {
        // Loop to allow the user to correct details if a duplicate is found
        while (true) {
            Optional<CreateModuleOnServerDialog.Result> result = CreateModuleOnServerDialog.show(owner, module.getBaseFileName(), campaignServerManager);
            if (result.isEmpty()) {
                return;
            }

            CreateModuleOnServerDialog.Result details = result.get();
            campaignServerManager.refreshJavaScriptTemplates();
            // Check for duplicate before hitting the server
            if (campaignServerManager.javaScriptTemplateExists(details.namespace(), details.name())) {
                errorReporter.reportError(
                        "A JavaScript template '" + details.namespace() + ":" + details.name() + "' already exists on the server. Please choose a different name.",
                        true);
                continue;
            }

            String canonicalFileName = details.name() + WorkspaceFileType.MODULE.getFileExtension();

            withBusyCursor(() -> {
                try {
                    EtmModuleRecord created = campaignServerManager.createJavaScriptTemplate(
                            details.namespace(), details.name(), details.label(), details.schemaKey(),
                            module.getWorkspaceFileContent());

                    EtmModuleSchemaKey key = new EtmModuleSchemaKey(details.name(), details.namespace());
                    module.setKey(key);

                    if (!module.getFileName().equals(canonicalFileName)) {
                        workspace.renameWorkspaceFile(module, canonicalFileName);
                    } else {
                        workspace.save();
                    }

                    errorReporter.logMessage("Successfully created " + fileTypeLower + " '" + details.namespace() + ":" + details.name() + "' on Campaign server!");
                    Platform.runLater(() -> fileOpenHandler.accept(module));

                } catch (ApiException apiException) {
                    errorReporter.reportError("An error occurred creating " + fileTypeLower + " '" + details.name() + "' on the Campaign server", apiException, true);
                }
            });
            return;
        }
    }

    private void withBusyCursor(Runnable operation) {
        Scene scene = sceneSupplier.get();
        scene.setCursor(Cursor.WAIT);
        Thread.ofVirtual().start(() -> {
            try {
                operation.run();
            } catch (Exception ex) {
                errorReporter.reportError(ex.getMessage() != null ? ex.getMessage() : "An error occurred during a Campaign operation", ex, true);
            } finally {
                Platform.runLater(() -> scene.setCursor(Cursor.DEFAULT));
            }
        });
    }

}