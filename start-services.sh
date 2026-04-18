#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export LOG_DIR="${SCRIPT_DIR}/logs"

echo "==> Building all modules..."
cd "$SCRIPT_DIR"
mvn -q package -DskipTests

mkdir -p "$LOG_DIR"/{orders-api,payments-api,inventory-api,mcp-server}

echo "==> Starting orders-api on :8081..."
java -jar "$SCRIPT_DIR/orders-api/target/orders-api-1.0.0-SNAPSHOT.jar" \
  --LOG_DIR="$LOG_DIR" \
  > /dev/null 2>&1 &
echo $! > "$SCRIPT_DIR/.pid-orders-api"

echo "==> Starting payments-api on :8082..."
java -jar "$SCRIPT_DIR/payments-api/target/payments-api-1.0.0-SNAPSHOT.jar" \
  --LOG_DIR="$LOG_DIR" \
  > /dev/null 2>&1 &
echo $! > "$SCRIPT_DIR/.pid-payments-api"

echo "==> Starting inventory-api on :8083..."
java -jar "$SCRIPT_DIR/inventory-api/target/inventory-api-1.0.0-SNAPSHOT.jar" \
  --LOG_DIR="$LOG_DIR" \
  > /dev/null 2>&1 &
echo $! > "$SCRIPT_DIR/.pid-inventory-api"

echo ""
echo "Services started. Waiting for them to become ready..."
sleep 8

for svc in orders-api payments-api inventory-api; do
    case $svc in
        orders-api)   port=8081 ;;
        payments-api)  port=8082 ;;
        inventory-api) port=8083 ;;
    esac
    if curl -sf "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
        echo "  [OK] ${svc} is healthy on :${port}"
    else
        echo "  [WARN] ${svc} did not respond on :${port} — check logs at ${LOG_DIR}/${svc}/app.log"
    fi
done

echo ""
echo "Logs: ${LOG_DIR}/"
echo ""
echo "To connect Claude Desktop, add to claude_desktop_config.json:"
echo '{'
echo '  "mcpServers": {'
echo '    "sre-copilot": {'
echo '      "command": "java",'
echo '      "args": ['
echo '        "-jar",'
echo "        \"${SCRIPT_DIR}/sre-mcp-server/target/sre-mcp-server-1.0.0-SNAPSHOT.jar\","
echo '        "--LOG_DIR='"${LOG_DIR}"'"'
echo '      ]'
echo '    }'
echo '  }'
echo '}'
