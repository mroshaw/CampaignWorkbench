package com.campaignworkbench.adobecampaignapi;

public class ConnectedStatus {
    private boolean isConnected;
    private CampaignInstance campaignInstance;

    public ConnectedStatus(boolean isConnected, CampaignInstance CampaignInstance) {
        this.isConnected = isConnected;
        this.campaignInstance = CampaignInstance;
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public String getConnectionName() {
        return campaignInstance != null && isConnected ? campaignInstance.getName() : "";
    }

    public String getConnectionHost() {
        return campaignInstance != null && isConnected ? CampaignServerManager.getHostName(campaignInstance.getEndpointUrl()) : "";
    }
}
