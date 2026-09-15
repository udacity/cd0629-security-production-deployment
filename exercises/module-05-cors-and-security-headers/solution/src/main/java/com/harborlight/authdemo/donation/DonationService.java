package com.harborlight.authdemo.donation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class DonationService {

    private final Map<Long, Donation> donations = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final CampaignService campaignService;

    public DonationService(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    public Donation createDonation(CreateDonationRequest request) {
        Long id = nextId.getAndIncrement();
        Donation donation = new Donation(id, request.campaignId(), request.donorName(), request.amountCents());
        campaignService.recordDonation(request.campaignId(), request.amountCents());
        donations.put(id, donation);
        return donation;
    }
}
