package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.ide.PickerDialog;
import javafx.collections.FXCollections;
import javafx.stage.Window;

import java.util.Optional;

public class CampaignModulePickerDialog extends PickerDialog<JavaScriptTemplate> {

    public static Optional<JavaScriptTemplate> show(Window owner) {
        return new CampaignModulePickerDialog().showInternal(owner);
    }

    public Optional<JavaScriptTemplate> showInternal(Window owner) {

        var modules = FXCollections.observableArrayList(
                CampaignServerManager.getAllJavaScriptTemplates(true)
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