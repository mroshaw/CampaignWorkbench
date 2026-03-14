package com.campaignworkbench.adobecampaignapi;

public class CampaignInstance {
    private int id;
    private String name;
    private CredentialStore credentials;

    public CampaignInstance(int id, String name) {
        this.id = id;
        this.name = name;
        this.credentials = new CredentialStore();
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void  setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
