package com.campaignworkbench.adobecampaignapi;


import java.util.Iterator;
import java.util.List;

public class CampaignInstances implements Iterable<CampaignInstance> {
    private List<CampaignInstance> instances;


    @Override
    public Iterator<CampaignInstance> iterator() {
        return instances.iterator();
    }
}
