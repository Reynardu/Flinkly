# Waschbär-Assets – Kurzanleitung

Alle Waschbären-Bilder werden mit **Adobe Firefly** (firefly.adobe.com → Text to Image) generiert
und als PNG in `android/app/src/main/res/drawable/` abgelegt.

---

## Einstellungen in Firefly

| Einstellung | Wert |
|---|---|
| Verhältnis | 1:1 |
| Content type | Graphic |
| Stil | Art → Clipart oder Cartoon |

Nach der Generierung: **Remove Background** → PNG mit transparentem Hintergrund speichern.

---

## Style-Anker (an jeden Prompt anhängen)

```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border,
flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks,
transparent background, sticker art style
```

---

## Prompts & Dateinamen

### Morgens (vor 10 Uhr)

**`raccoon_morning_sleepy.png`** – Verschlafen mit Kaffee
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — half-closed sleepy eyes, holding a steaming coffee cup with both paws, messy bedhead fur, small zzz floating above head, sitting upright
```

**`raccoon_morning_yawning.png`** – Gähnend mit Schlafmütze
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — wide open yawning mouth, eyes closed, wearing a blue sleeping cap with pompom, sitting, one paw raised to mouth
```

---

### Gestern keine Aufgaben erledigt

**`raccoon_lazy_laundry.png`** – Im Wäscheberg
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — sitting in a pile of laundry, socks and clothes around it, guilty embarrassed expression, looking sideways
```

**`raccoon_lazy_dishwasher.png`** – Schaut aus Spülmaschine
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — peeking head and paws over an open dishwasher door, wide surprised guilty eyes, dishes visible inside, embarrassed expression
```

---

### Tagesziel erreicht

**`raccoon_done_broom.png`** – Stolz mit Besen
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — big happy smile, holding a broom upright, standing proudly, sparkle stars around, satisfied expression
```

**`raccoon_done_celebrate.png`** – Feiert mit Partyhat
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — wearing a colorful party hat, both arms raised in celebration, big joyful eyes, confetti falling around, huge smile
```

---

### Haushaltspause

**`raccoon_paused_sunglasses.png`** – Sonnenbrille, entspannt
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — wearing round sunglasses, relaxed smile, lying on back with paws behind head, looking carefree
```

**`raccoon_paused_hammock.png`** – In der Hängematte
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — lying in an orange hammock, eyes closed peacefully, one leg dangling over the side, small smile, zzz above head
```

---

### Guter Fortschritt (≥ 50 % Tagesziel)

**`raccoon_progress_motivated.png`** – Motiviert mit Checkliste
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — holding a clipboard with checkmarks, determined expression, slightly narrowed focused eyes, standing upright, small sweat drop on forehead
```

**`raccoon_progress_cleaning.png`** – Beim Wischen
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — actively mopping the floor, concentrated expression, sweat drops, holding a mop with both paws, leaning forward with effort
```

---

### Tagesstart (normaler Tag)

**`raccoon_ready_checklist.png`** – Schaut auf To-do-Liste
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — holding a small to-do list, bright alert eyes, neutral friendly smile, standing straight, pencil tucked behind ear
```

**`raccoon_ready_supplies.png`** – Mit Putzmitteln
```
cute chibi cartoon raccoon, full body, thick black outlines, white sticker border, flat pastel colors, gray fur, black eye mask, striped tail, pink cheeks, transparent background, sticker art style — standing next to a cleaning bucket, holding a spray bottle, cheerful wide eyes, big smile, ready-to-work pose
```

---

## Ablage in der App

Generierte PNGs umbenennen (Kleinschreibung, Unterstriche, kein Leerzeichen) und ablegen unter:

```
android/app/src/main/res/drawable/<dateiname>.png
```

Die alten XML-Platzhalter (`raccoon_*.xml`) können danach gelöscht werden —
Android bevorzugt automatisch die PNG-Datei wenn Name übereinstimmt.

---

## Konsistenz-Tipps

- Immer denselben Style-Anker verwenden → einheitliches Aussehen
- Alle Bilder in **1:1** generieren
- Hintergrund immer per **Remove Background** entfernen bevor speichern
- Dateigröße: PNG unter 500 KB anstreben (ggf. mit TinyPNG komprimieren)
