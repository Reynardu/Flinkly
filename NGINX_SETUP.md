# Nginx Setup für Flinkly

Der Backend-Container läuft auf Port `8000`. Nginx leitet Anfragen an ihn weiter.

---

## 1. Nginx-Konfiguration hinzufügen

Neue Datei in eurer bestehenden Nginx-Konfiguration (z.B. `/etc/nginx/sites-available/flinkly`):

```nginx
server {
    listen 80;
    server_name flinkly.ut.reynardus.dev;

    # Weiterleitung auf HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name flinkly.ut.reynardus.dev;

    ssl_certificate     /etc/letsencrypt/live/flinkly.ut.reynardus.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/flinkly.ut.reynardus.dev/privkey.pem;

    # REST API
    location /api/ {
        proxy_pass         http://localhost:8000/;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }

    # WebSocket (wichtig: upgrade headers!)
    location /ws/ {
        proxy_pass         http://localhost:8000/ws/;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection "upgrade";
        proxy_set_header   Host $host;
        proxy_read_timeout 3600s;
    }
}
```

Aktivieren:
```bash
ln -s /etc/nginx/sites-available/flinkly /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

---

## 2. SSL-Zertifikat (Let's Encrypt)

```bash
apt install certbot python3-certbot-nginx
certbot --nginx -d flinkly.ut.reynardus.dev
```

Das Zertifikat wird automatisch alle 90 Tage erneuert.

---

## 3. Backend starten

```bash
# .env aus .env.example kopieren und anpassen
cp .env.example .env
nano .env

# Container starten
docker-compose up -d

# Datenbank-Migration ausführen (einmalig & nach Updates)
docker-compose exec backend alembic upgrade head
```

---

## 4. In der App eintragen

In den App-Einstellungen unter **Server-URL** eintragen:

| Situation | URL |
|---|---|
| Von außen (empfohlen) | `https://flinkly.ut.reynardus.dev` |
| Nur Heimnetz | `http://192.168.x.x:8000` |

---

## 5. Health-Check testen

```bash
curl https://flinkly.ut.reynardus.dev/api/health
# Erwartete Antwort: {"status": "ok"}
```
