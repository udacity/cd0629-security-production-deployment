package com.harborlight.authdemo.donation;

public record CreateDonationRequest(Long campaignId, String donorName, long amountCents) {
}
