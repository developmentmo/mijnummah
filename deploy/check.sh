#!/usr/bin/env bash
# Monitoring from the server itself, every 5 minutes from cron:
#   */5 * * * *  cd /opt/mijnummah/deploy && ./check.sh >> backups/check.log 2>&1
# Checks backend, portal, HTTPS, free disk space and the age of the newest backup. With HEALTHCHECK_PING_URL it pings
# an external monitor (healthchecks.io style): <url> when everything is fine, <url>/fail otherwise. That monitor
# also notices when this server stops pinging altogether. Exit code 1 when a check fails.
set -uo pipefail
cd "$(dirname "$0")" || exit 1
set -a
# shellcheck disable=SC1091
[ -f .env ] && . ./.env
set +a

problems=()
probe() {
  local name=$1 url=$2 expect=$3
  curl -fsS --max-time 10 "$url" 2> /dev/null | grep -q "$expect" || problems+=("$name")
}

probe backend "http://127.0.0.1:${BACKEND_PORT:-8080}/actuator/health/readiness" '"UP"'
probe portaal "http://127.0.0.1:${PORTAL_PORT:-8081}/healthz" '^ok'
[ -z "${API_BASE_URL:-}" ] || probe "api-https" "${API_BASE_URL}/actuator/health" '"UP"'
[ -z "${PORTAL_ORIGIN:-}" ] || probe "portaal-https" "${PORTAL_ORIGIN}/healthz" '^ok'

disk_max=${DISK_MAX_PERCENT:-85}
for path in / "${DOCKER_ROOT:-/var/lib/docker}"; do
  [ -d "$path" ] || continue
  used=$(df -P "$path" | awk 'NR == 2 { sub(/%/, "", $5); print $5 }')
  [ "${used:-0}" -le "$disk_max" ] || problems+=("schijf-$path-${used}%")
done

backup_dir=${BACKUP_DIR:-./backups}
max_age_minutes=$(( ${BACKUP_MAX_AGE_HOURS:-26} * 60 ))
if [ -z "$(find "$backup_dir" -maxdepth 1 -name 'mijnummah-*.dump' -mmin -"$max_age_minutes" 2> /dev/null | head -1)" ]; then
  problems+=("back-up-ouder-dan-${BACKUP_MAX_AGE_HOURS:-26}u")
fi

now=$(date -u +%FT%TZ)
if [ ${#problems[@]} -eq 0 ]; then
  echo "$now ok"
  [ -z "${HEALTHCHECK_PING_URL:-}" ] || curl -fsS --max-time 10 --retry 3 -o /dev/null "$HEALTHCHECK_PING_URL" || true
  exit 0
fi

summary="${problems[*]}"
echo "$now FOUT $summary"
[ -z "${HEALTHCHECK_PING_URL:-}" ] \
  || curl -fsS --max-time 10 --retry 3 -o /dev/null --data-raw "$summary" "${HEALTHCHECK_PING_URL%/}/fail" || true
exit 1
