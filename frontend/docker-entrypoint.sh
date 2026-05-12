#!/bin/sh
set -e

LISTEN_PORT="${PORT:-80}"
sed "s/__PORT__/${LISTEN_PORT}/g" /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

if [ -n "$API_BASE_URL" ]; then
  cat > /usr/share/nginx/html/runtime-config.js <<EOCONFIG
window.__APP_CONFIG__ = {
  apiBaseUrl: "$API_BASE_URL"
};
EOCONFIG
else
  cat > /usr/share/nginx/html/runtime-config.js <<EOCONFIG
window.__APP_CONFIG__ = {
  apiBaseUrl: null
};
EOCONFIG
fi

exec nginx -g "daemon off;"
