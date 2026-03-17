package com.campaignworkbench.adobecampaignapi;

import javafx.beans.property.SimpleBooleanProperty;

public class CampaignConnectionManager {
    private final SimpleBooleanProperty campaignConnectionStatus = new SimpleBooleanProperty(false);

    public SimpleBooleanProperty getConnectionStatusObservable() {
        return campaignConnectionStatus;
    }

    public void setConnectionStatus(boolean value) {
        campaignConnectionStatus.set(value);
    }
}
