#!/usr/bin/env bash
# Launched by Claude Desktop as the MCP server process.
# Using a wrapper ensures the log directory exists and JVM system properties are set
# before Spring Boot / Logback initialize — command-line args arrive too late for logback.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"
JAR="${SCRIPT_DIR}/sre-mcp-server/target/sre-mcp-server-1.0.0-SNAPSHOT.jar"

mkdir -p "${LOG_DIR}/mcp-server"

exec /Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home/bin/java \
  -Dlog.dir="${LOG_DIR}" \
  -jar "${JAR}"
