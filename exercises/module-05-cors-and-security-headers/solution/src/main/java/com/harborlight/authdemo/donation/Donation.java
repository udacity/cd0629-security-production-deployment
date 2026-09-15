package com.harborlight.authdemo.donation;

public record Donation(Long id, Long campaignId, String donorName, long amountCents) {
}
