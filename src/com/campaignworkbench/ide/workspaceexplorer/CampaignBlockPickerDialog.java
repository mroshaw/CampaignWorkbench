package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlock;
import com.campaignworkbench.ide.PickerDialog;
import javafx.collections.FXCollections;
import javafx.stage.Window;

import java.util.Optional;

public class CampaignBlockPickerDialog extends PickerDialog<PersonalizationBlock> {

    public static Optional<PersonalizationBlock> show(Window owner) {
        return new CampaignBlockPickerDialog().showInternal(owner);
    }

    public Optional<PersonalizationBlock> showInternal(Window owner) {

        var blocks = FXCollections.observableArrayList(
                CampaignServerManager.getAllPersoBlocks(true)
                        .getPersonalisationBlocks()
        );

        return showDialog(
                owner,
                "Pick Personalization Block",
                "Select a block",
                blocks,
                b -> String.format("%s%s (%s)",
                        b.getFolder().getFullName(),
                        b.getLabel(),
                        b.getName()),
                1200,
                700
        );
    }
}