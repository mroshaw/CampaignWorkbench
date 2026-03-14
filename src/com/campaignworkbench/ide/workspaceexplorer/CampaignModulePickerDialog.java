package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.EtmModuleRecord;
import com.campaignworkbench.ide.dialogs.PickerDialog;
import javafx.collections.FXCollections;
import javafx.stage.Window;

import java.util.Optional;

public class CampaignModulePickerDialog extends PickerDialog<EtmModuleRecord> {

    public static Optional<EtmModuleRecord> show(Window owner, CampaignServerManager campaignServerManager) {
        return new CampaignModulePickerDialog().showInternal(owner, campaignServerManager);
    }

    public Optional<EtmModuleRecord> showInternal(Window owner, CampaignServerManager campaignServerManager) {

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