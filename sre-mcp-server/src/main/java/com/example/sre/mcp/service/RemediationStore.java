package com.example.sre.mcp.service;

import com.example.sre.mcp.config.ServiceDefinition;
import com.example.sre.mcp.model.RemediationProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RemediationStore {

    private static final Logger log = LoggerFactory.getLogger(RemediationStore.class);
    private static final int PROPOSAL_TTL_SECONDS = 300;
    private static final String SUPPORTED_ACTION = "restart";

    private final ConcurrentHashMap<String, RemediationProposal> proposals = new ConcurrentHashMap<>();
    private final RestClient restClient;

    public RemediationStore(RestClient restClient) {
        this.restClient = restClient;
    }

    public RemediationProposal createProposal(ServiceDefinition svc, String action) {
        if (!SUPPORTED_ACTION.equals(action)) {
            throw new IllegalArgumentException("Unsupported action '" + action + "'. Supported: restart");
        }
        String id = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(PROPOSAL_TTL_SECONDS);
        String description = "Restart " + svc.name() + " by calling POST " + svc.baseUrl() + svc.restartEndpoint()
                + ". This will reset the service state. Proposal expires in " + PROPOSAL_TTL_SECONDS + "s.";
        RemediationProposal proposal = new RemediationProposal(id, svc.name(), action, description, expiresAt);
        proposals.put(id, proposal);
        log.info("Remediation proposal created: id={} service={} action={}", id, svc.name(), action);
        return proposal;
    }

    public record ExecutionResult(boolean executed, String proposalId, String outcome) {}

    public ExecutionResult execute(String proposalId, ServiceDefinition svc) {
        RemediationProposal proposal = proposals.get(proposalId);

        if (proposal == null) {
            throw new IllegalArgumentException("Proposal '" + proposalId + "' not found");
        }
        if (proposal.isExpired()) {
            proposals.remove(proposalId);
            throw new IllegalStateException("Proposal '" + proposalId + "' has expired");
        }
        if (!proposal.markConsumed()) {
            throw new IllegalStateException("Proposal '" + proposalId + "' was already executed");
        }

        try {
            String response = restClient.post()
                    .uri(svc.baseUrl() + svc.restartEndpoint())
                    .retrieve()
                    .body(String.class);
            log.info("Remediation executed: proposal_id={} service={} response={}", proposalId, svc.name(), response);
            return new ExecutionResult(true, proposalId, "Service " + svc.name() + " restarted. Response: " + response);
        } catch (Exception e) {
            log.error("Remediation execution failed: proposal_id={} service={} error={}", proposalId, svc.name(), e.getMessage());
            throw new RuntimeException("Restart call failed for " + svc.name() + ": " + e.getMessage());
        }
    }

    public Optional<RemediationProposal> find(String proposalId) {
        return Optional.ofNullable(proposals.get(proposalId));
    }

    @Scheduled(fixedRate = 60000)
    void sweepExpired() {
        int before = proposals.size();
        proposals.entrySet().removeIf(e -> e.getValue().isExpired() || e.getValue().isConsumed());
        int removed = before - proposals.size();
        if (removed > 0) {
            log.info("Swept {} expired/consumed remediation proposal(s)", removed);
        }
    }
}
