FROM nginx:1.27-alpine

# Копируем конфиг и файлы
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY . /usr/share/nginx/html

# Создаём entrypoint скрипт, который подставит переменные ПЕРЕД запуском nginx
RUN apk add --no-cache envsubst

COPY <<'EOF' /docker-entrypoint.sh
#!/bin/sh
set -e

# Подставляем API_BASE_URL в runtime-config.js
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

# Запускаем nginx
exec nginx -g "daemon off;"
EOF

RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]

