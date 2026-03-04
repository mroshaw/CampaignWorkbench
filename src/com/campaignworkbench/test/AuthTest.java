package com.campaignworkbench.test;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;

public class AuthTest {

    static void main(String[] args) {

        try {
            boolean connected =  CampaignServerManager.connect();

            if(connected) {
                System.out.println("Connected!");
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}
