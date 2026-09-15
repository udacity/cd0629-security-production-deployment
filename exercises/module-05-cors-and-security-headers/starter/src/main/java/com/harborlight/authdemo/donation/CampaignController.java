package com.harborlight.authdemo.donation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public List<Campaign> list() {
        return campaignService.listCampaigns();
    }

    @GetMapping("/{id}")
    public Campaign get(@PathVariable Long id) {
        return campaignService.getCampaign(id);
    }
}
