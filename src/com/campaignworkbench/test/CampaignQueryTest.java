package com.campaignworkbench.test;

import com.campaignworkbench.adobecampaignapi.CampaignServerManager;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlock;
import com.campaignworkbench.adobecampaignapi.schemas.PersonalizationBlockCollection;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplate;
import com.campaignworkbench.adobecampaignapi.schemas.JavaScriptTemplateCollection;

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

            Optional<JavaScriptTemplate> template = campaignServerManager.getJavaScriptTemplate("ssg", "ETM_M00_END.js");
            template.ifPresent(javaScriptTemplate -> javaScriptTemplate.print(false));

            Optional<PersonalizationBlock> block = campaignServerManager.getPersonalizationBlock(680205044);
            block.ifPresent(includeView -> includeView.print(true));

            // IncludeViewCollection blockCollection = CampaignServerManager.getAllPersoBlocks(true);
            // JavaScriptTemplateCollection javascriptCollection = CampaignServerManager.getAllJavaScriptTemplates(true);
            // printJavaScriptTemplates(javascriptCollection);
            // printPersonBlocks(blockCollection);


        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    private static void printPersonBlocks(PersonalizationBlockCollection blockCollection) {
        for (PersonalizationBlock view : blockCollection.getPersonalisationBlocks()) {
            view.print(false);
        }
    }


    private static void printJavaScriptTemplates(JavaScriptTemplateCollection javascriptTemplateCollection) {
        for (JavaScriptTemplate template : javascriptTemplateCollection.getJavaScriptTemplates()) {
            template.print(false);
        }
    }

}
