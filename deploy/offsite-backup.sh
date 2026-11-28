#!/usr/bin/env bash
# Encrypted copy of BACKUP_DIR outside this server with restic (run in a container, nothing to install).
# The repository (RESTIC_REPOSITORY, e.g. an S3 bucket in the EU) is created on the first run.
#   ./offsite-backup.sh                 # copy, then remove snapshots older than OFFSITE_KEEP_DAYS, then check
#   ./offsite-backup.sh snapshots       # list what is stored
#   ./offsite-backup.sh restore <dir>   # newest snapshot into a new, empty directory
# In cron right after backup.sh:
#   15 2 * * *  cd /opt/mijnummah/deploy && ./backup.sh >> backups/backup.log 2>&1 && ./offsite-backup.sh >> backups/backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")"
set -a
# shellcheck disable=SC1091
[ -f .env ] && . ./.env
set +a

: "${RESTIC_REPOSITORY:?RESTIC_REPOSITORY is leeg (zie .env.example)}"
: "${RESTIC_PASSWORD:?RESTIC_PASSWORD is leeg (zie .env.example)}"
BACKUP_DIR=${BACKUP_DIR:-./backups}
OFFSITE_KEEP_DAYS=${OFFSITE_KEEP_DAYS:-30}
RESTIC_IMAGE=${RESTIC_IMAGE:-restic/restic:0.18.0}
backups=$(cd "$BACKUP_DIR" && pwd)

# Secrets go to the container by name (-e NAME), so they never appear in the process list or the log.
restic() {
  docker run --rm -i --hostname mijnummah \
    -e RESTIC_REPOSITORY -e RESTIC_PASSWORD -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_DEFAULT_REGION \
    -v "$backups":/backups:ro -v mijnummah-restic-cache:/root/.cache/restic \
    "$@"
}

case "${1:-backup}" in
  backup)
    if ! restic "$RESTIC_IMAGE" cat config > /dev/null 2>&1; then
      echo "$(date -u +%FT%TZ) restic-repository aanmaken"
      restic "$RESTIC_IMAGE" init
    fi
    restic "$RESTIC_IMAGE" backup /backups --tag mijnummah --exclude '*.partial' --exclude '*.log' --quiet
    restic "$RESTIC_IMAGE" forget --tag mijnummah --keep-within "${OFFSITE_KEEP_DAYS}d" --prune --quiet
    restic "$RESTIC_IMAGE" check --quiet
    echo "$(date -u +%FT%TZ) externe back-up bijgewerkt"
    ;;
  snapshots)
    restic "$RESTIC_IMAGE" snapshots --tag mijnummah
    ;;
  restore)
    target=${2:?Gebruik: ./offsite-backup.sh restore <nieuwe map>}
    if [ -e "$target" ] && [ -n "$(ls -A "$target")" ]; then
      echo "$target bestaat al en is niet leeg; kies een nieuwe map." >&2
      exit 1
    fi
    mkdir -p "$target"
    target=$(cd "$target" && pwd)
    docker run --rm --hostname mijnummah \
      -e RESTIC_REPOSITORY -e RESTIC_PASSWORD -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_DEFAULT_REGION \
      -v "$target":/restore "$RESTIC_IMAGE" restore latest --tag mijnummah --target /restore
    echo "Hersteld in $target/backups; zet een dump terug met ./restore.sh <dump> <nieuwe database>."
    ;;
  *)
    echo "Gebruik: ./offsite-backup.sh [backup|snapshots|restore <map>]" >&2
    exit 2
    ;;
esac
