#!/usr/bin/env bash
# Restores a backup into a NEW, empty database next to the live one, so it can be checked first:
#   ./restore.sh backups/mijnummah-20261011T021500Z.dump mijnummah_restore
# The script refuses an existing database name and never touches the live database. Switching the backend
# to the restored database is a deliberate manual step, described in DEPLOYMENT.md.
set -euo pipefail
cd "$(dirname "$0")"
set -a; [ -f .env ] && . ./.env; set +a

dump=${1:?usage: restore.sh <dump-file> <new-database-name>}
database=${2:?usage: restore.sh <dump-file> <new-database-name>}
COMPOSE=${COMPOSE:-docker compose -f docker-compose.prod.yml}

if ! [[ "$database" =~ ^[a-z_][a-z0-9_]{0,62}$ ]]; then
    echo "invalid database name: $database" >&2
    exit 1
fi
if [ "$database" = "${POSTGRES_DB:-mijnummah}" ]; then
    echo "refusing to restore into the live database ${POSTGRES_DB:-mijnummah}" >&2
    exit 1
fi
exists=$($COMPOSE exec -T postgres sh -c "psql -U \"\$POSTGRES_USER\" -d postgres -tAc \"select 1 from pg_database where datname = '$database'\"")
if [ -n "$exists" ]; then
    echo "database $database already exists; choose a new name" >&2
    exit 1
fi

$COMPOSE exec -T postgres sh -c "createdb -U \"\$POSTGRES_USER\" '$database'"
$COMPOSE exec -T postgres sh -c "pg_restore -U \"\$POSTGRES_USER\" -d '$database' --no-owner --exit-on-error" < "$dump"
echo "restored $dump into $database"
$COMPOSE exec -T postgres sh -c "psql -U \"\$POSTGRES_USER\" -d '$database' -c \"select version, description, success from flyway_schema_history order by installed_rank desc limit 3\""
