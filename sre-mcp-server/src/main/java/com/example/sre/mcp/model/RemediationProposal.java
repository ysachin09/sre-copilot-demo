package com.example.sre.mcp.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemediationProposal {

    private final String proposalId;
    private final String service;
    private final String action;
    private final String description;
    private final Instant expiresAt;
    private final AtomicBoolean consumed = new AtomicBoolean(false);

    public RemediationProposal(String proposalId, String service, String action, String description, Instant expiresAt) {
        this.proposalId = proposalId;
        this.service = service;
        this.action = action;
        this.description = description;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean markConsumed() {
        return consumed.compareAndSet(false, true);
    }

    public boolean isConsumed() {
        return consumed.get();
    }

    public String proposalId() { return proposalId; }
    public String service() { return service; }
    public String action() { return action; }
    public String description() { return description; }
    public String expiresAt() { return expiresAt.toString(); }
}
