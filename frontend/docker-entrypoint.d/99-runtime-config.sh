#!/usr/bin/env sh
set -eu

CONFIG_PATH="/usr/share/nginx/html/runtime-config.js"

API_BASE_URL="${API_BASE_URL:-}"

if [ -z "$API_BASE_URL" ]; then
  API_BASE_URL_JSON="null"
else
  API_BASE_URL_JSON="\"$API_BASE_URL\""
fi

cat > "$CONFIG_PATH" <<EOF
globalThis.__RUNTIME_CONFIG__ = {
  API_BASE_URL: $API_BASE_URL_JSON
};
EOF

