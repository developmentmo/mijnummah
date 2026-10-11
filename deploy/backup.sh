#!/usr/bin/env bash
# Daily database backup: a compressed pg_dump (custom format) in BACKUP_DIR, older ones removed after
# BACKUP_KEEP_DAYS. Run from cron on the server, for example:
#   15 2 * * *  cd /opt/mijnummah/deploy && ./backup.sh >> backups/backup.log 2>&1
# Copy BACKUP_DIR to storage outside this server as well (see DEPLOYMENT.md); a backup on the same disk is no backup.
set -euo pipefail
cd "$(dirname "$0")"
set -a; [ -f .env ] && . ./.env; set +a

BACKUP_DIR=${BACKUP_DIR:-./backups}
BACKUP_KEEP_DAYS=${BACKUP_KEEP_DAYS:-30}
COMPOSE=${COMPOSE:-docker compose -f docker-compose.prod.yml}

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
target="$BACKUP_DIR/mijnummah-$(date -u +%Y%m%dT%H%M%SZ).dump"
partial="$target.partial"

$COMPOSE exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner' > "$partial"
# A dump that cannot be listed is not usable; keep the previous ones in that case.
$COMPOSE exec -T postgres pg_restore --list < "$partial" > /dev/null
mv "$partial" "$target"
chmod 600 "$target"
echo "$(date -u +%FT%TZ) backup written: $target ($(du -h "$target" | cut -f1))"

find "$BACKUP_DIR" -maxdepth 1 -name 'mijnummah-*.dump' -type f -mtime +"$BACKUP_KEEP_DAYS" -print -delete
