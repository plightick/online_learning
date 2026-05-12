FROM nginx:1.27-alpine

COPY nginx.conf.template /etc/nginx/conf.d/default.conf.template
COPY . /usr/share/nginx/html
RUN rm -f /usr/share/nginx/html/nginx.conf.template \
  && rm -f /usr/share/nginx/html/docker-entrypoint.sh

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
