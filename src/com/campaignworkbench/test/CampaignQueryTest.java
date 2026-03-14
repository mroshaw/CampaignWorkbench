package com.campaignworkbench.test;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.*;

import java.util.Optional;

public class CampaignQueryTest {

    static void main(String[] args) {

        try {

            CampaignServerManager campaignServerManager = new CampaignServerManager();
            boolean connected =  campaignServerManager.connect();

            if(connected) {
                System.out.println("Connected!");
            }

            campaignServerManager.refreshAll();

            EtmModuleSchemaKey testKey = new EtmModuleSchemaKey("ssg", "\"ETM_M00_END.js\"");

            Optional<EtmModuleRecord> template = campaignServerManager.getJavaScriptTemplate(testKey);
            template.ifPresent(javaScriptTemplate -> javaScriptTemplate.print(false));

            Optional<PersoBlockRecord> block = campaignServerManager.getPersonalizationBlock(680205044);
            block.ifPresent(includeView -> includeView.print(true));

            // IncludeViewCollection blockCollection = CampaignServerManager.getAllPersoBlocks(true);
            // JavaScriptTemplateCollection javascriptCollection = CampaignServerManager.getAllJavaScriptTemplates(true);
            // printJavaScriptTemplates(javascriptCollection);
            // printPersonBlocks(blockCollection);


        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    private static void printPersonBlocks(PersoBlockSchema blockCollection) {
        for (PersoBlockRecord view : blockCollection.getPersonalisationBlocks()) {
            view.print(false);
        }
    }


    private static void printJavaScriptTemplates(EtmModuleSchema javascriptTemplateCollection) {
        for (EtmModuleRecord template : javascriptTemplateCollection.getJavaScriptTemplates()) {
            template.print(false);
        }
    }

}
