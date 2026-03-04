package com.campaignworkbench.test;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.IncludeView;
import com.campaignworkbench.adobecampaignapi.schemas.IncludeViewCollection;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplateCollection;

import java.util.Optional;

public class CampaignQueryTest {

    static void main(String[] args) {

        try {

            boolean connected =  CampaignServerManager.connect();

            if(connected) {
                System.out.println("Connected!");
            }

            CampaignServerManager.refreshAll();

            Optional<JavaScriptTemplate> template = CampaignServerManager.getScriptTemplate("ssg", "ETM_M00_END.js");
            template.ifPresent(javaScriptTemplate -> javaScriptTemplate.print(false));

            Optional<IncludeView> block = CampaignServerManager.getPersonalizationBlock(680205044);
            block.ifPresent(includeView -> includeView.print(false));

            // IncludeViewCollection blockCollection = CampaignServerManager.getAllPersoBlocks(true);
            // JavaScriptTemplateCollection javascriptCollection = CampaignServerManager.getAllJavaScriptTemplates(true);
            // printJavaScriptTemplates(javascriptCollection);
            // printPersonBlocks(blockCollection);


        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    private static void printPersonBlocks(IncludeViewCollection blockCollection) {
        for (IncludeView view : blockCollection.getIncludeViews()) {
            view.print(false);
        }
    }


    private static void printJavaScriptTemplates(JavaScriptTemplateCollection javascriptTemplateCollection) {
        for (JavaScriptTemplate template : javascriptTemplateCollection.getJavaScriptTemplates()) {
            template.print(false);
        }
    }

}
