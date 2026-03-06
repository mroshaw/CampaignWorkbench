package com.campaignworkbench.test;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;

public class AuthTest {

    static void main(String[] args) {

        try {
            CampaignServerManager campaignServerManager = new CampaignServerManager();
            boolean connected =  campaignServerManager.connect();

            if(connected) {
                System.out.println("Connected!");
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}
