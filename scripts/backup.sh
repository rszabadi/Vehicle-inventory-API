#!/usr/bin/env bash
set -euo pipefail
umask 077

cd "$(dirname "$0")/.."
DOCKER=${DOCKER:-docker}
mkdir -p backups
out="backups/swiss_$(date +%F_%H%M).sql.gz"

$DOCKER compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip > "$out.tmp"
mv "$out.tmp" "$out"

find backups -name '*.sql.gz' -mtime +14 -delete
echo "Backup written: $out"
