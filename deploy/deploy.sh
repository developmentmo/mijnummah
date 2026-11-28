#!/usr/bin/env bash
# Puts a release (git tag) live on this server: checks the configuration, makes a backup, checks out the tag,
# builds and starts the stack, waits for the healthchecks and runs a smoke test. See DEPLOYMENT.md, "Release".
#   ./deploy.sh v1.0.0
# Rollback without a new migration: ./deploy.sh <previous tag>. With a migration: see DEPLOYMENT.md, "Rollback".
set -euo pipefail

# Everything is inside main, so bash has read the whole script before "git checkout" replaces this file.
main() {
  cd "$(dirname "$0")"
  local tag=${1:-}
  if [[ ! "$tag" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
    echo "Gebruik: ./deploy.sh <tag>   (bijvoorbeeld v1.0.0)" >&2
    exit 2
  fi
  local compose=${COMPOSE:-docker compose -f docker-compose.prod.yml}
  local git=${GIT:-git -C ..}

  ./preflight.sh

  if ! $git diff --quiet || ! $git diff --cached --quiet; then
    echo "De repository op de server heeft lokale wijzigingen; zet die eerst opzij (git status)." >&2
    exit 1
  fi
  $git fetch --tags --quiet origin
  if ! $git rev-parse --quiet --verify "refs/tags/$tag^{commit}" > /dev/null; then
    echo "Tag $tag bestaat niet." >&2
    exit 1
  fi

  local previous
  previous=$(sed -n 's/^IMAGE_TAG=//p' .env | tail -1)
  echo "== $(date -u +%FT%TZ) van ${previous:-?} naar $tag"

  if $compose ps --status running --services 2> /dev/null | grep -qx postgres; then
    echo "== back-up vóór de release"
    ./backup.sh
  else
    echo "== geen draaiende database; eerste installatie, dus geen back-up"
  fi

  echo "== $tag uitchecken en starten"
  $git checkout --quiet "$tag"
  if ! IMAGE_TAG="$tag" $compose up -d --build --wait --wait-timeout 300; then
    failed "$previous" "de containers werden niet gezond"
  fi
  set_image_tag "$tag"

  echo "== rooktest"
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
  smoke_test || failed "$previous" "de rooktest faalde"
  echo "== $(date -u +%FT%TZ) $tag draait"
}

set_image_tag() {
  local tmp
  tmp=$(mktemp .env.XXXXXX)
  if grep -q '^IMAGE_TAG=' .env; then
    sed "s/^IMAGE_TAG=.*/IMAGE_TAG=$1/" .env > "$tmp"
  else
    { cat .env; echo "IMAGE_TAG=$1"; } > "$tmp"
  fi
  chmod 600 "$tmp"
  mv "$tmp" .env
}

# Retries for a while: on a first start Caddy still has to fetch the certificates.
check() {
  local name=$1 url=$2 expect=$3
  for _ in $(seq 1 "${SMOKE_ATTEMPTS:-24}"); do
    if curl -fsS --max-time 10 "$url" 2> /dev/null | grep -q "$expect"; then
      echo "ok   $name"
      return 0
    fi
    sleep "${SMOKE_DELAY:-5}"
  done
  echo "FOUT $name ($url)"
  return 1
}

smoke_test() {
  local ok=0
  check "backend" "http://127.0.0.1:${BACKEND_PORT:-8080}/actuator/health/readiness" '"UP"' || ok=1
  check "publieke API" "http://127.0.0.1:${BACKEND_PORT:-8080}/api/v1/public/mosques" '"items"' || ok=1
  check "portaal" "http://127.0.0.1:${PORTAL_PORT:-8081}/healthz" '^ok' || ok=1
  check "API via HTTPS" "${API_BASE_URL}/actuator/health" '"UP"' || ok=1
  check "portaal via HTTPS" "${PORTAL_ORIGIN}/config.json" "${API_BASE_URL}" || ok=1
  return $ok
}

failed() {
  local previous=$1 reason=$2
  echo "== MISLUKT: $reason" >&2
  echo "Logs: docker compose -f docker-compose.prod.yml logs --tail 200 backend" >&2
  if [ -n "$previous" ] && [ "$previous" != latest ]; then
    echo "Terug naar de vorige versie (alleen als deze release geen nieuwe migratie heeft): ./deploy.sh $previous" >&2
  fi
  exit 1
}

main "$@"
exit
