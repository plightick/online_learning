FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY . /usr/share/nginx/html

RUN chmod +x /usr/share/nginx/html/docker-entrypoint.d/99-runtime-config.sh \
  && mkdir -p /docker-entrypoint.d \
  && cp /usr/share/nginx/html/docker-entrypoint.d/99-runtime-config.sh /docker-entrypoint.d/99-runtime-config.sh \
  && chmod +x /docker-entrypoint.d/99-runtime-config.sh

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

