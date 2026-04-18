#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"
JAR="${SCRIPT_DIR}/sre-mcp-server/target/sre-mcp-server-1.0.0-SNAPSHOT.jar"
CONFIG_DIR="${HOME}/Library/Application Support/Claude"
CONFIG="${CONFIG_DIR}/claude_desktop_config.json"

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: JAR not found at ${JAR}"
    echo "       Run ./start-services.sh first to build the project."
    exit 1
fi

mkdir -p "$CONFIG_DIR"

python3 - "$CONFIG" "$JAR" "$LOG_DIR" <<'EOF'
import sys, json, os

config_path, jar, log_dir = sys.argv[1], sys.argv[2], sys.argv[3]

if os.path.exists(config_path):
    with open(config_path) as f:
        config = json.load(f)
else:
    config = {}

config.setdefault("mcpServers", {})
config["mcpServers"]["sre-copilot"] = {
    "command": "java",
    "args": [
        "-jar", jar,
        f"--LOG_DIR={log_dir}"
    ]
}

with open(config_path, "w") as f:
    json.dump(config, f, indent=2)

print(f"Written to: {config_path}")
EOF

echo ""
echo "Done. Restart Claude Desktop for the change to take effect."
echo "The 'sre-copilot' MCP server will appear in Claude Desktop's tool list."
