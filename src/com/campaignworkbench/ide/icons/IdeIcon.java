package com.campaignworkbench.ide.icons;

import javafx.scene.Node;

public enum IdeIcon {

    // Main Toolbar Icons
    OPEN_WORKSPACE("Folder_Open"),
    NEW_WORKSPACE("Folder_Add"),
    CLOSE_WORKSPACE("Folder_Prohibited"),
    CLOSE_ALL_TABS("Tabs"),
    RUN_TEMPLATE("Play"),

    // Workspace Explorer icons
    WORKSPACE("Folder"),
    TEMPLATE("Document_Flowchart"),
    MODULE("Document_Table"),
    BLOCK("Document_Javascript"),
    CONTEXT("Document_Database"),

    // Workspace Explorer Toolbar icons
    CONNECT("Plug_Connected"),
    DISCONNECT("Plug_Disconnected"),
    NEW_FROM_CAMPAIGN("Document_Arrow_Down"),
    REFRESH_FROM_CAMPAIGN("Document_Arrow_Left"),
    UPDATE_TO_CAMPAIGN("Document_Arrow_Right"),
    NEW_FILE("Document_Add"),
    ADD_FILE("Document_Link"),
    DELETE_FILE("Document_Dismiss"),
    SET_DATA_CONTEXT("Channel_Add"),
    CLEAR_DATA_CONTEXT("Channel_Dismiss"),
    SET_MESSAGE_CONTEXT("Mail_Add"),
    CLEAR_MESSAGE_CONTEXT("Mail_Dismiss"),

    // Tab Toolbar
    FORMAT_CODE("Text_Grammar_Wand"),
    FOLD_ALL("Text_Collapse"),
    UNFOLD_ALL("Text_Expand"),
    WRAP_TEXT("Text_Wrap"),
    // Find Toolbar
    FIND_START("Search"),
    FIND_CLEAR("Dismiss_Circle"),
    FOLDER_SYNC("Folder_Sync");

    private final String iconName;

    IdeIcon(String iconName) {
        this.iconName = iconName;
    }

    public Node getIcon(int size, String cssClass, boolean filled) {
        Node iconImage = FluentIcons.icon(iconName, size, filled);
        iconImage.getStyleClass().clear();
        iconImage.getStyleClass().add(cssClass);
        return iconImage;
    }
}