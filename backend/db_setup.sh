#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_DIR/.env"

# .env prüfen
if [ ! -f "$ENV_FILE" ]; then
    echo "FEHLER: $ENV_FILE nicht gefunden."
    echo "Bitte zuerst anlegen:"
    echo "  cp $PROJECT_DIR/.env.example $ENV_FILE"
    echo "  nano $ENV_FILE"
    exit 1
fi

# Pflichtfelder prüfen
source "$ENV_FILE"
MISSING=()
[ -z "$POSTGRES_PASSWORD" ] && MISSING+=("POSTGRES_PASSWORD")
[ -z "$SECRET_KEY" ]         && MISSING+=("SECRET_KEY")
[ -z "$HOUSEHOLD_PASSWORD" ] && MISSING+=("HOUSEHOLD_PASSWORD")

if [ ${#MISSING[@]} -gt 0 ]; then
    echo "FEHLER: Folgende Variablen fehlen in der .env:"
    for var in "${MISSING[@]}"; do
        echo "  - $var"
    done
    exit 1
fi

echo "==> .env OK"

# Docker starten (falls noch nicht läuft)
cd "$PROJECT_DIR"
echo "==> Starte Container..."
docker compose up -d --build

# Warten bis db healthy ist
echo "==> Warte auf Datenbank..."
for i in $(seq 1 30); do
    STATUS=$(docker compose ps --format json db 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('Health',''))" 2>/dev/null || echo "")
    if [ "$STATUS" = "healthy" ]; then
        break
    fi
    sleep 2
    echo "   ... ($i/30)"
done

# Alembic Migration
echo "==> Führe Datenbank-Migration aus..."
docker compose exec backend alembic upgrade head

echo ""
echo "Fertig! Health-Check:"
curl -sf http://localhost:40501/health && echo "" || echo "Backend noch nicht erreichbar – kurz warten und erneut prüfen."
