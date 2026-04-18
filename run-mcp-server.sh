#!/usr/bin/env bash
# Launched by Claude Desktop as the MCP server process.
# Using a wrapper ensures the log directory exists and JVM system properties are set
# before Spring Boot / Logback initialize — command-line args arrive too late for logback.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export LOG_DIR="${SCRIPT_DIR}/logs"
JAR="${SCRIPT_DIR}/sre-mcp-server/target/sre-mcp-server-1.0.0-SNAPSHOT.jar"

mkdir -p "${LOG_DIR}/mcp-server"

exec java -jar "${JAR}"
