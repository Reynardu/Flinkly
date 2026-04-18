# Flinkly – Einrichtungsanleitung

## Übersicht

| Komponente | Technologie | Port |
|---|---|---|
| Backend (API) | FastAPI + Python | 40501 |
| Datenbank | PostgreSQL 16 | intern (Docker) |
| Reverse Proxy | Nginx | 80 / 443 |
| Domain | `flinkly.ut.reynardus.dev` | — |

---

## Teil 1 – DMZ VM (Server)

### Voraussetzungen

```bash
# Docker installieren
curl -fsSL https://get.docker.com | sh
usermod -aG docker $USER   # eigenen User zur docker-Gruppe hinzufügen
newgrp docker

# Docker Compose (Plugin)
apt install -y docker-compose-plugin

# Nginx + Certbot
apt install -y nginx certbot python3-certbot-nginx git
```

---

### 1. Projekt auf den Server übertragen

```bash
# Option A – Git (empfohlen)
git clone <dein-repo-url> /opt/flinkly
cd /opt/flinkly

# Option B – SCP von deinem Entwicklungsrechner
scp -r D:\Entwicklung\HaushaltsSoftware user@<server-ip>:/opt/flinkly
```

---

### 2. Umgebungsvariablen konfigurieren

```bash
cd /opt/flinkly
nano .env
```

Inhalt der `.env`:

```env
# Datenbank
POSTGRES_DB=flinkly
POSTGRES_USER=flinkly
POSTGRES_PASSWORD=<sicheres-passwort-hier>

# App
DATABASE_URL=postgresql://flinkly:<sicheres-passwort-hier>@db:5432/flinkly
SECRET_KEY=<langer-zufaelliger-string>          # z.B.: openssl rand -hex 32
HOUSEHOLD_PASSWORD=<haushalt-passwort>           # Passwort zum Registrieren neuer Nutzer

# Optional (für Push-Benachrichtigungen)
FIREBASE_PROJECT_ID=
FCM_SERVER_KEY=
```

> **Tipp:** `openssl rand -hex 32` erzeugt einen sicheren Zufallsstring für `SECRET_KEY`.

---

### 3. Backend starten

```bash
cd /opt/flinkly
docker compose up -d --build
```

Container-Status prüfen:

```bash
docker compose ps
# Beide Container (db + backend) müssen "healthy" bzw. "running" sein
```

Logs anzeigen:

```bash
docker compose logs -f backend
```

---

### 4. Datenbank-Migration ausführen

**Einmalig beim ersten Start** und nach jedem Update:

```bash
docker compose exec backend alembic upgrade head
```

Erwartete Ausgabe:
```
INFO  [alembic.runtime.migration] Running upgrade  -> 001, add household_pauses table
```

---

### 5. Nginx konfigurieren

```bash
nano /etc/nginx/sites-available/flinkly
```

```nginx
server {
    listen 80;
    server_name flinkly.ut.reynardus.dev;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name flinkly.ut.reynardus.dev;

    ssl_certificate     /etc/letsencrypt/live/flinkly.ut.reynardus.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/flinkly.ut.reynardus.dev/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;

    # REST API
    location / {
        proxy_pass         http://localhost:40501/;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }

    # WebSocket
    location /ws/ {
        proxy_pass         http://localhost:40501/ws/;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection "upgrade";
        proxy_set_header   Host $host;
        proxy_read_timeout 3600s;
    }
}
```

```bash
# Aktivieren und testen
ln -s /etc/nginx/sites-available/flinkly /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

---

### 6. SSL-Zertifikat (Let's Encrypt)

```bash
certbot --nginx -d flinkly.ut.reynardus.dev
```

Das Zertifikat wird automatisch alle 90 Tage erneuert. Erneuerung testen:

```bash
certbot renew --dry-run
```

---

### 7. Health-Check

```bash
curl https://flinkly.ut.reynardus.dev/health
# Erwartete Antwort: {"status":"ok"}
```

API-Dokumentation (Swagger UI) ist erreichbar unter:
```
https://flinkly.ut.reynardus.dev/docs
```

---

### 8. Autostart nach Neustart

Docker Compose startet die Container automatisch neu (`restart: unless-stopped` ist bereits gesetzt). Sicherstellen, dass Docker beim Systemstart läuft:

```bash
systemctl enable docker
```

---

### Updates einspielen

```bash
cd /opt/flinkly
git pull
docker compose up -d --build
docker compose exec backend alembic upgrade head
```

---

## Teil 2 – Android App (Handy)

### APK installieren

1. **APK bauen** (auf dem Entwicklungsrechner in Android Studio):
   - `Build` → `Generate Signed App Bundle / APK` → `APK` → Release
   - Oder Debug-APK: `Build` → `Build APK(s)`

2. **APK auf das Handy übertragen**:
   - Per USB-Kabel oder
   - Per ADB: `adb install app-release.apk`

3. **Installation erlauben**:
   - Android-Einstellungen → Sicherheit → **Unbekannte Quellen** aktivieren (einmalig)
   - Die APK-Datei im Datei-Manager antippen und installieren

---

### Erste Einrichtung in der App

#### Schritt 1 – Server verbinden

Beim ersten Start erscheint die **Server-URL-Eingabe**:

| Verbindung | URL |
|---|---|
| Von überall (empfohlen) | `https://flinkly.ut.reynardus.dev` |
| Nur im Heimnetz | `http://<server-ip>:40501` |

„Verbinden" antippen → die App prüft ob der Server erreichbar ist.

---

#### Schritt 2 – Konto erstellen oder einloggen

**Neues Konto (Registrieren):**
1. Tab „Registrieren" auswählen
2. Anzeigename eingeben (z.B. „Armin")
3. Haushalt-Passwort eingeben (das `HOUSEHOLD_PASSWORD` aus der `.env`)
4. „Konto erstellen" antippen
5. **Login-Code notieren** – dieser Code ist der einzige Weg, sich auf neuen Geräten einzuloggen

**Vorhandenes Konto (Einloggen):**
1. Tab „Einloggen" auswählen
2. Gespeicherten Login-Code eingeben
3. „Einloggen" antippen

---

#### Schritt 3 – Haushalt erstellen oder beitreten

**Neuen Haushalt erstellen:**
1. Tab „Erstellen" auswählen
2. Namen eingeben (z.B. „WG Musterstraße")
3. „Haushalt erstellen" antippen

**Bestehendem Haushalt beitreten:**
1. Einladungslink vom Haushaltsmitglied anfordern
   - Das Mitglied geht in **Einstellungen → Haushalt → Einladungslink erstellen**
2. Tab „Beitreten" auswählen
3. Den Link oder Token einfügen
4. „Beitreten" antippen

---

#### Schritt 4 – Räume und Aufgaben anlegen

1. Unten in der Navigation auf **Räume** tippen
2. **+** antippen → Raum mit Name, Symbol und Farbe erstellen
3. Raum antippen → Aufgaben für diesen Raum anlegen
4. Aufgabe erledigen: Häkchen-Symbol antippen → Punkte werden gutgeschrieben

---

### Login-Code sichern

> Der Login-Code ist das einzige Anmeldedatum. Ohne ihn ist kein Login auf neuen Geräten möglich.

Den Code findest du jederzeit unter **Einstellungen → Login-Code** (Auge-Symbol zum Einblenden).

**Empfehlung:** Code in einem Passwortmanager speichern.

---

### Haushaltspausen eintragen

Für Urlaub oder besondere Tage unter **Einstellungen → Haushaltspausen → +**:
- Start- und Enddatum im Format `YYYY-MM-DD` eingeben (z.B. `2026-07-01`)
- Optionalen Grund angeben (z.B. „Sommerurlaub")

Während einer aktiven Pause:
- Der **Streak wird nicht unterbrochen**
- Aufgaben können weiterhin freiwillig erledigt werden
- Auf dem Dashboard erscheint ein blauer Pause-Banner

---

## Troubleshooting

| Problem | Lösung |
|---|---|
| App zeigt „Server nicht erreichbar" | URL prüfen, `curl https://flinkly.ut.reynardus.dev/health` auf Server testen |
| Container startet nicht | `docker compose logs backend` — fehlende `.env`-Variablen? |
| Datenbank-Fehler beim Start | `docker compose exec backend alembic upgrade head` ausführen |
| Login-Code vergessen | Kein Reset möglich — neues Konto registrieren |
| Zertifikat abgelaufen | `certbot renew` manuell ausführen |
