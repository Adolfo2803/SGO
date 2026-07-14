#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Respaldo de SGO: base de datos + almacén de PDFs
# Uso: ./scripts/respaldo.sh [directorio_destino]
# ============================================================

DESTINO="${1:-/opt/sgo/respaldos}"
RETENCION_DIAS=30
FECHA=$(date +%Y-%m-%d_%H%M)
COMPOSE_FILE="docker-compose.prod.yml"
DIRECTORIO_SGO="$(cd "$(dirname "$0")/.." && pwd)"

mkdir -p "$DESTINO"

echo "=== Respaldo SGO — $FECHA ==="

echo "1/3 Respaldando base de datos..."
docker compose -f "$DIRECTORIO_SGO/$COMPOSE_FILE" exec -T db \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists \
    | gzip > "$DESTINO/sgo-db-$FECHA.sql.gz"
echo "    → $DESTINO/sgo-db-$FECHA.sql.gz"

echo "2/3 Respaldando almacén de PDFs..."
ALMACEN_MOUNT=$(docker compose -f "$DIRECTORIO_SGO/$COMPOSE_FILE" \
    exec -T app printenv SGO_ALMACEN_RAIZ 2>/dev/null || echo "/data/almacen")
docker compose -f "$DIRECTORIO_SGO/$COMPOSE_FILE" exec -T app \
    tar cf - -C "$ALMACEN_MOUNT" . \
    | gzip > "$DESTINO/sgo-archivos-$FECHA.tar.gz"
echo "    → $DESTINO/sgo-archivos-$FECHA.tar.gz"

echo "3/3 Eliminando respaldos con más de $RETENCION_DIAS días..."
find "$DESTINO" -name "sgo-*.gz" -mtime +"$RETENCION_DIAS" -delete -print \
    | while read -r f; do echo "    eliminado: $f"; done

echo "=== Respaldo completado ==="
