package com.example.sre.mcp.tools;

import com.example.sre.mcp.config.ServiceDefinition;
import com.example.sre.mcp.config.ServiceRegistry;
import com.example.sre.mcp.model.RemediationProposal;
import com.example.sre.mcp.model.ToolError;
import com.example.sre.mcp.service.RemediationStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RemediationTool {

    private final ServiceRegistry registry;
    private final RemediationStore remediationStore;

    public RemediationTool(ServiceRegistry registry, RemediationStore remediationStore) {
        this.registry = registry;
        this.remediationStore = remediationStore;
    }

    @Tool(description = """
            Propose a remediation action for a service. This DOES NOT execute anything.
            It creates a pending proposal with an ID that must be passed to approve_and_execute.
            Supported actions: restart
            Returns: proposalId, service, action, description, expiresAt (5 minutes from now).
            Always present this proposal to the user and wait for explicit approval before executing.
            """)
    public Map<String, Object> propose_remediation(String service, String action) {
        if (!registry.exists(service)) {
            return Map.of("error", ToolError.notFound("Service '" + service + "' is not registered"));
        }

        ServiceDefinition svc = registry.find(service).get();
        try {
            RemediationProposal proposal = remediationStore.createProposal(svc, action);
            return Map.of(
                    "proposalId", proposal.proposalId(),
                    "service", proposal.service(),
                    "action", proposal.action(),
                    "description", proposal.description(),
                    "expiresAt", proposal.expiresAt(),
                    "status", "PENDING_APPROVAL"
            );
        } catch (IllegalArgumentException e) {
            return Map.of("error", ToolError.invalidArgument(e.getMessage()));
        }
    }

    @Tool(description = """
            Execute a previously proposed remediation action after human approval.
            Pass the proposalId returned by propose_remediation.
            Proposals expire after 5 minutes and are single-use — executing twice will fail.
            Returns: executed (boolean), proposalId, outcome message.
            """)
    public Map<String, Object> approve_and_execute(String proposalId) {
        if (proposalId == null || proposalId.isBlank()) {
            return Map.of("error", ToolError.invalidArgument("proposalId must not be empty"));
        }

        var proposalOpt = remediationStore.find(proposalId);
        if (proposalOpt.isEmpty()) {
            return Map.of("error", ToolError.notFound("Proposal '" + proposalId + "' not found or already expired"));
        }

        RemediationProposal proposal = proposalOpt.get();
        if (!registry.exists(proposal.service())) {
            return Map.of("error", ToolError.internal("Service '" + proposal.service() + "' no longer registered"));
        }

        ServiceDefinition svc = registry.find(proposal.service()).get();
        try {
            RemediationStore.ExecutionResult result = remediationStore.execute(proposalId, svc);
            return Map.of(
                    "executed", result.executed(),
                    "proposalId", result.proposalId(),
                    "outcome", result.outcome()
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Map.of("error", ToolError.invalidArgument(e.getMessage()));
        } catch (Exception e) {
            return Map.of("error", ToolError.backendDegraded(e.getMessage()));
        }
    }
}
