#!/usr/bin/env bash
# Checks deploy/.env before a (first) start: required values, HTTPS addresses, mail, push, secrets and file rights.
# Prints only the names of settings, never their values. Exit code 1 when something must be fixed first.
#   ./preflight.sh                  # deploy.sh runs it too
set -euo pipefail
cd "$(dirname "$0")"
ENV_FILE=${ENV_FILE:-.env}

errors=0
warnings=0
error() { echo "FOUT:          $*"; errors=$((errors + 1)); }
warn() { echo "WAARSCHUWING:  $*"; warnings=$((warnings + 1)); }

if [ ! -f "$ENV_FILE" ]; then
  echo "FOUT:          $ENV_FILE ontbreekt; begin met: cp .env.example .env && chmod 600 .env"
  exit 1
fi
rights=$(stat -c %a "$ENV_FILE" 2>/dev/null || stat -f %Lp "$ENV_FILE")
[ "$rights" = 600 ] || error "$ENV_FILE heeft rechten $rights; zet ze op 600 (chmod 600 $ENV_FILE)"

set -a
# shellcheck disable=SC1090
. "./$ENV_FILE"
set +a

value() { printf '%s' "${!1:-}"; }
required() { [ -n "$(value "$1")" ] || error "$1 is leeg"; }

required POSTGRES_PASSWORD
[ -z "${POSTGRES_PASSWORD:-}" ] || [ ${#POSTGRES_PASSWORD} -ge 16 ] \
  || error "POSTGRES_PASSWORD is korter dan 16 tekens (maak er een met: openssl rand -base64 32)"
required JWT_SECRET
[ -z "${JWT_SECRET:-}" ] || [ "$(printf '%s' "$JWT_SECRET" | wc -c)" -ge 32 ] \
  || error "JWT_SECRET is korter dan 32 bytes (maak er een met: openssl rand -base64 48)"

for name in PORTAL_ORIGIN API_BASE_URL; do
  url=$(value "$name")
  if [ -z "$url" ]; then
    error "$name is leeg"
  elif [[ "$url" != https://* ]]; then
    error "$name begint niet met https://"
  elif [[ "$url" == */ ]]; then
    error "$name eindigt op een /; laat die weg"
  fi
done

if [[ ",${COMPOSE_PROFILES:-}," == *,caddy,* ]]; then
  required API_DOMAIN
  required PORTAL_DOMAIN
  required ACME_EMAIL
  for pair in "API_BASE_URL API_DOMAIN" "PORTAL_ORIGIN PORTAL_DOMAIN"; do
    read -r url domain <<< "$pair"
    if [ -n "$(value "$domain")" ] && [ "$(value "$url")" != "https://$(value "$domain")" ]; then
      error "$url hoort https://<$domain> te zijn"
    fi
  done
else
  warn "COMPOSE_PROFILES bevat geen caddy: zorg dat een reverse proxy op de server HTTPS doet (zie DEPLOYMENT.md)"
fi

case "${MAIL_PROVIDER:-none}" in
  smtp)
    required MAIL_FROM
    required SPRING_MAIL_HOST
    required SPRING_MAIL_USERNAME
    required SPRING_MAIL_PASSWORD
    ;;
  none) warn "MAIL_PROVIDER=none: geen wachtwoordherstel en geen uitnodigingen per e-mail" ;;
  *) error "MAIL_PROVIDER moet smtp of none zijn" ;;
esac

case "${PUSH_PROVIDER:-none}" in
  fcm)
    required FCM_PROJECT_ID
    required FCM_CREDENTIALS_FILE
    if [ -n "${FCM_CREDENTIALS_FILE:-}" ]; then
      if [[ "$FCM_CREDENTIALS_FILE" != /run/secrets/* ]]; then
        error "FCM_CREDENTIALS_FILE moet in /run/secrets/ staan (dat is SECRETS_DIR in de container)"
      else
        key="${SECRETS_DIR:-./secrets}/${FCM_CREDENTIALS_FILE#/run/secrets/}"
        if [ ! -f "$key" ]; then
          error "het FCM-sleutelbestand ontbreekt in SECRETS_DIR"
        else
          # The backend runs as uid 10001; only that user may read the key.
          rights=$(stat -c %a "$key" 2>/dev/null || stat -f %Lp "$key")
          owner=$(stat -c %u "$key" 2>/dev/null || stat -f %u "$key")
          if [ "$owner" != 10001 ] || { [ "$rights" != 600 ] && [ "$rights" != 400 ]; }; then
            error "het FCM-sleutelbestand moet van uid 10001 zijn met rechten 600" \
              "(sudo chown 10001:10001 <bestand> && sudo chmod 600 <bestand>)"
          fi
        fi
      fi
    fi
    ;;
  none) warn "PUSH_PROVIDER=none: moskeeën kunnen geen pushmeldingen sturen" ;;
  *) error "PUSH_PROVIDER moet fcm of none zijn" ;;
esac

[ -z "${API_DOCS_ENABLED:-}" ] || error "API_DOCS_ENABLED hoort in productie niet gezet te zijn"
[ -z "${BOOTSTRAP_ADMIN_PASSWORD:-}" ] \
  || warn "BOOTSTRAP_ADMIN_PASSWORD staat nog in $ENV_FILE; haal het weg zodra de eerste platformbeheerder bestaat"
[ "${IMAGE_TAG:-latest}" != latest ] || warn "IMAGE_TAG=latest; deploy.sh zet hier de versie die draait"
[ -n "${RESTIC_REPOSITORY:-}" ] && [ -n "${RESTIC_PASSWORD:-}" ] \
  || warn "geen externe back-up ingesteld (RESTIC_REPOSITORY, RESTIC_PASSWORD)"
[ -n "${HEALTHCHECK_PING_URL:-}" ] || warn "geen externe bewaking ingesteld (HEALTHCHECK_PING_URL)"

echo "Controle klaar: $errors fout(en), $warnings waarschuwing(en)."
[ "$errors" -eq 0 ]
