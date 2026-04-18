# Design Insights from This Project

## 1. Human-in-the-loop via propose → approve → execute
Destructive actions (restarts, rollbacks) are never executed in one step. The MCP server first creates a **proposal** — a short-lived token with a description of what will happen. Claude shows it to the user and waits for explicit confirmation before calling `approve_and_execute`. This pattern is reusable anywhere an AI might take irreversible action.

**Key details that make it safe:**
- 5-minute TTL — a stale approval can't be used later
- Single-use flag — calling execute twice on the same proposal fails
- Proposal is stored server-side, not passed back and forth through Claude — Claude can't tamper with it

## 2. AI reasons, tools compute — never mix the two
Health status (UP_HEALTHY / UP_DEGRADED / DOWN) is computed by deterministic Java code, not by asking the LLM to interpret logs. Claude receives a structured result and reasons about it. This separation means:
- Health checks are testable and auditable
- Claude can't hallucinate a health status
- The same tool can be called by any AI model or script

The rule: **tools produce facts, the AI produces judgment**.

## 3. Tool descriptions are the AI's API contract
The `@Tool` description is not documentation — it's the interface. Claude reads it to decide whether and how to call the tool. A vague description leads to wrong tool selection or wrong arguments. This project treats descriptions like method contracts: they state what the tool does, what inputs mean, and what output values represent.

## 4. stdio transport removes an entire security surface
By using stdin/stdout instead of HTTP, the MCP server has no open port, no authentication to implement, no TLS to manage. Only the process that spawned it (Claude Desktop) can talk to it. This is a meaningful simplification for local tooling.

## 5. Structured logs as the observability layer
Instead of a separate metrics system, health is derived from log files written in JSON (Logstash format). This means:
- No extra infrastructure (no Prometheus, no metrics endpoints)
- The same logs humans read are the source of truth for the AI
- Adding observability to a new service is just adding a log line

The tradeoff: log-based health has a latency equal to the log flush interval, and a 5-minute window means a brand-new error spike won't show immediately.

## 6. Chaos as a first-class config, not a code hack
The payments service failure rate is a single YAML property (`payments.chaos.failure-rate: 0.30`). Resetting it is a single HTTP call to `/admin/restart`. This makes the demo reproducible and the "incident" controllable — you can turn the problem on and off without touching code or redeploying.

## 7. Adding a new monitored service requires zero code changes
Services are declared in `application.yml` under `sre.services`. The `ServiceRegistry` loads them at startup. All four tools (discovery, health, logs, remediation) work against any registered service automatically. The extension point is config, not code.

## 8. stdout hygiene is a hard contract in stdio MCP
Any output to stdout — Spring's startup banner, a stray `System.out.println`, a logging misconfiguration — silently breaks the MCP protocol. The project enforces this through three independent layers: `banner-mode: off`, `web-application-type: none`, and an empty console log pattern. One layer failing still has two backups.
