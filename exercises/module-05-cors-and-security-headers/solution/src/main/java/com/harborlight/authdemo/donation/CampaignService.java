package com.harborlight.authdemo.donation;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class CampaignService {

    private final Map<Long, Campaign> campaigns = new ConcurrentHashMap<>();

    public CampaignService() {
        campaigns.put(1L, new Campaign(1L, "Clean Water for Fernbrook Village", 2_000_000, 875_000));
        campaigns.put(2L, new Campaign(2L, "Rebuild the Ridgeline Community Library", 500_000, 412_500));
    }

    public List<Campaign> listCampaigns() {
        return List.copyOf(campaigns.values());
    }

    public Campaign getCampaign(Long id) {
        Campaign campaign = campaigns.get(id);
        if (campaign == null) {
            throw new NoSuchElementException("No campaign with id " + id);
        }
        return campaign;
    }

    void recordDonation(Long campaignId, long amountCents) {
        campaigns.compute(campaignId, (id, campaign) -> {
            if (campaign == null) {
                throw new NoSuchElementException("No campaign with id " + id);
            }
            return campaign.withRaisedCents(campaign.raisedCents() + amountCents);
        });
    }
}
