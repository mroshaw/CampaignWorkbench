package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.ide.PickerDialog;
import javafx.collections.FXCollections;
import javafx.stage.Window;

import java.util.Optional;

public class CampaignModulePickerDialog extends PickerDialog<JavaScriptTemplate> {

    public static Optional<JavaScriptTemplate> show(Window owner, CampaignServerManager campaignServerManager) {
        return new CampaignModulePickerDialog().showInternal(owner, campaignServerManager);
    }

    public Optional<JavaScriptTemplate> showInternal(Window owner, CampaignServerManager campaignServerManager) {

        var modules = FXCollections.observableArrayList(
                campaignServerManager.getAllJavaScriptTemplates(true)
                        .getJavaScriptTemplates()
        );

        return showDialog(
                owner,
                "Pick JavaScript Module",
                "Select a module",
                modules,
                m -> String.format("%s:%s", m.getNamespace(), m.getName()),
                1000,
                700
        );
    }
}