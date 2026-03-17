package com.campaignworkbench.ide.workspaceexplorer;

import com.campaignworkbench.ide.dialogs.PickerDialog;
import com.campaignworkbench.workspace.WorkspaceFile;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.util.Optional;
public class WorkspaceFilePickerDialog<T extends WorkspaceFile> extends PickerDialog<T> {

    // new static wrapper
    public static <T extends WorkspaceFile> Optional<T> show(
            Stage owner,
            ObservableList<T> workspaceFiles
    ) {
        WorkspaceFilePickerDialog<T> dialog = new WorkspaceFilePickerDialog<>();
        return dialog.showInternal(owner, workspaceFiles);
    }

    public Optional<T> showInternal(
            Stage owner,
            ObservableList<T> files
    ) {

        return showDialog(
                owner,
                "Pick File",
                "Select a workspace file",
                files,
                WorkspaceFile::getFileName,
                900,
                600
        );
    }
}