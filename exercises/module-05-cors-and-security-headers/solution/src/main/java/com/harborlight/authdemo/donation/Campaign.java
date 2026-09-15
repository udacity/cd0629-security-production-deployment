package com.harborlight.authdemo.donation;

public record Campaign(Long id, String title, long goalCents, long raisedCents) {

    public Campaign withRaisedCents(long newRaisedCents) {
        return new Campaign(id, title, goalCents, newRaisedCents);
    }
}
