# Productie-image van het beheerportaal. Bouwen vanuit deze map:  docker build -t mijnummah-webportaal .
# Starten:  docker run -e API_BASE_URL=https://api.example.nl -p 8080:8080 mijnummah-webportaal

FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY docker/default.conf.template docker/security-headers.inc.template /etc/nginx/templates/
COPY --chmod=0755 docker/15-check-api-base-url.sh /docker-entrypoint.d/
COPY --from=build /src/dist/webportaal/browser /usr/share/nginx/html
# De standaardconfiguratie van het image wordt vervangen door de template hierboven.
USER root
RUN rm -f /etc/nginx/conf.d/default.conf /usr/share/nginx/html/config.json
USER 101
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
