#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for svc in orders-api payments-api inventory-api; do
    pidfile="$SCRIPT_DIR/.pid-${svc}"
    if [[ -f "$pidfile" ]]; then
        pid=$(cat "$pidfile")
        if kill -0 "$pid" 2>/dev/null; then
            echo "Stopping ${svc} (pid ${pid})..."
            kill "$pid"
        fi
        rm -f "$pidfile"
    fi
done

echo "All services stopped."
