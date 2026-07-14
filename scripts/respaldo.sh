#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Respaldo de SGO: base de datos + almacén de PDFs
# Uso: ./scripts/respaldo.sh
# Configuración via .env en la raíz del proyecto.
# Solo requiere que el contenedor 'db' esté corriendo.
# Los PDFs se respaldan desde el volumen Docker directamente,
# sin necesidad de que la aplicación esté levantada.
#
# NOTA — Git Bash en Windows (MSYS2):
#   Git Bash convierte automáticamente los argumentos que parecen
#   rutas Unix (/data, /var/...) a rutas Windows cuando se pasan
#   a ejecutables nativos como docker.exe. Eso rompe rutas que son
#   INTERNAS del contenedor Linux y no deben convertirse.
#   Este script usa dos defensas:
#     - Rutas relativas para el compose file (no empiezan con /,
#       Git Bash no las toca).
#     - MSYS_NO_PATHCONV=1 como prefijo en los comandos docker que
#       pasan rutas internas de contenedor (/data).
#   NO se debe poner MSYS_NO_PATHCONV=1 de forma global: rompe
#   la conversión de rutas del host que Docker sí necesita.
# ============================================================

DIRECTORIO_SGO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$DIRECTORIO_SGO"

COMPOSE_FILE="docker-compose.prod.yml"

log() { echo "[$(date +%H:%M:%S)] $*"; }

# --- 1. Cargar .env -----------------------------------------------------------

if [ ! -f .env ]; then
    echo "ERROR: no se encontró .env en $DIRECTORIO_SGO" >&2
    echo "Copie .env.example a .env y configure las variables antes de respaldar." >&2
    exit 1
fi

set -a
# shellcheck source=/dev/null
source .env
set +a

# --- 2. Validar variables requeridas -----------------------------------------

REQUERIDAS=(POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD)
faltantes=()
for var in "${REQUERIDAS[@]}"; do
    if [ -z "${!var:-}" ]; then
        faltantes+=("$var")
    fi
done

if [ ${#faltantes[@]} -gt 0 ]; then
    echo "ERROR: faltan variables requeridas en .env:" >&2
    for var in "${faltantes[@]}"; do
        echo "  - $var" >&2
    done
    exit 1
fi

# --- 3. Verificar prerrequisitos ----------------------------------------------

if ! docker compose -f "$COMPOSE_FILE" exec -T db \
        pg_isready -U "$POSTGRES_USER" >/dev/null 2>&1; then
    log "ERROR: la base de datos no está disponible." >&2
    log "Verifique que el contenedor 'db' esté corriendo:" >&2
    log "  docker compose -f $COMPOSE_FILE up -d db" >&2
    exit 1
fi

PROYECTO=${COMPOSE_PROJECT_NAME:-$(basename "$DIRECTORIO_SGO" | tr '[:upper:]' '[:lower:]')}
VOLUMEN_PDF="${PROYECTO}_sgo-almacen"

if ! docker volume inspect "$VOLUMEN_PDF" >/dev/null 2>&1; then
    log "ERROR: el volumen '$VOLUMEN_PDF' no existe." >&2
    log "¿Se ha levantado alguna vez con docker compose -f $COMPOSE_FILE up?" >&2
    exit 1
fi

# --- 4. Preparar destino y directorio temporal --------------------------------

DESTINO="${SGO_RESPALDO_DIR:-$HOME/sgo-respaldos}"
RETENCION_DIAS=30
FECHA=$(date +%Y-%m-%d_%H%M)

ARCHIVO_DB="sgo-db-$FECHA.sql.gz"
ARCHIVO_PDF="sgo-archivos-$FECHA.tar.gz"

DIR_TEMP=$(mktemp -d)
trap 'rm -rf "$DIR_TEMP"' EXIT

mkdir -p "$DESTINO"

log "=== Respaldo SGO — $FECHA ==="
log "Destino: $DESTINO"

# --- 5. Respaldar BD (pg_dump DENTRO del contenedor, sin exponer contraseña) --

log "1/3 Respaldando base de datos..."
docker compose -f "$COMPOSE_FILE" exec -T db \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists \
    | gzip > "$DIR_TEMP/$ARCHIVO_DB"

if [ ! -s "$DIR_TEMP/$ARCHIVO_DB" ]; then
    log "FALLO: respaldo de BD resultó vacío." >&2
    exit 1
fi

# --- 6. Respaldar PDFs (contenedor efímero; app NO necesita estar corriendo) --
#    MSYS_NO_PATHCONV=1 evita que Git Bash convierta /data (ruta interna del
#    contenedor Linux) a C:/Program Files/Git/data. En Linux no tiene efecto.

log "2/3 Respaldando almacén de PDFs..."
MSYS_NO_PATHCONV=1 \
docker run --rm -v "${VOLUMEN_PDF}:/data:ro" alpine:3 \
    tar cf - -C /data . \
    | gzip > "$DIR_TEMP/$ARCHIVO_PDF"

if [ ! -s "$DIR_TEMP/$ARCHIVO_PDF" ]; then
    log "FALLO: respaldo de PDFs resultó vacío." >&2
    exit 1
fi

# --- 7. Mover al destino final (solo si ambos están completos) ----------------

mv "$DIR_TEMP/$ARCHIVO_DB" "$DESTINO/$ARCHIVO_DB"
mv "$DIR_TEMP/$ARCHIVO_PDF" "$DESTINO/$ARCHIVO_PDF"

# --- 8. Limpieza de respaldos antiguos ----------------------------------------

log "3/3 Eliminando respaldos con más de $RETENCION_DIAS días..."
find "$DESTINO" -name "sgo-*.gz" -mtime +"$RETENCION_DIAS" -delete -print \
    | while read -r f; do log "    eliminado: $f"; done || true

# --- Resumen ------------------------------------------------------------------

TAMANO_DB=$(du -h "$DESTINO/$ARCHIVO_DB" | cut -f1)
TAMANO_PDF=$(du -h "$DESTINO/$ARCHIVO_PDF" | cut -f1)

log "=== ÉXITO: respaldo completado ==="
log "  BD:  $DESTINO/$ARCHIVO_DB ($TAMANO_DB)"
log "  PDF: $DESTINO/$ARCHIVO_PDF ($TAMANO_PDF)"
