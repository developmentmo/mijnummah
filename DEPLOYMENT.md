# Deployment en beheer

Hoe het platform in productie draait: configuratie, geheimen, eerste start, back-ups en herstel, logging en
monitoring, healthchecks en de release- en rollbackprocedure. Bestanden staan in [`deploy/`](deploy/).

Stand: fase 6 (11 oktober 2026). Er is nog geen productieomgeving; de stack is lokaal en in CI gedraaid (zie
"Wat is getest").

## Opzet

Eén Linux-server (VPS) met Docker en Docker Compose. Voor de pilot is één server genoeg.

```
internet ──HTTPS──> reverse proxy op de server (Caddy/nginx/Traefik, TLS-certificaten)
                      ├── api.<domein>     -> 127.0.0.1:8080  backend (Spring Boot)
                      └── beheer.<domein>  -> 127.0.0.1:8081  webportaal (nginx)
                                             postgres (alleen in het Docker-netwerk)
```

- `backend/Dockerfile`: bouwt met Maven, draait op Temurin 21 JRE als gebruiker zonder rechten (uid 10001), met
  healthcheck op `/actuator/health/readiness`.
- `frontend/webportaal/Dockerfile`: bouwt de Angular-app en serveert die met `nginx-unprivileged` (uid 101, poort
  8080). `config.json` komt uit de omgevingsvariabele `API_BASE_URL`, dus één image voor elke omgeving. Beveiligings-
  headers en Content-Security-Policy met een nonce per verzoek staan in `frontend/webportaal/docker/`.
- `deploy/docker-compose.prod.yml`: PostgreSQL 17, backend en portaal, met healthchecks, `restart: unless-stopped`,
  logrotatie, en poorten alleen op `127.0.0.1`.

De Flutter-app wordt niet op de server gezet; die gaat via de App Store en Google Play (fase 7 en de lijst
"Productieblokkades").

## Eerste installatie

1. Server met Docker Engine en de Compose-plugin; firewall alleen 22 (SSH, bij voorkeur met sleutel), 80 en 443 open.
2. DNS: `api.<domein>` en `beheer.<domein>` naar de server.
3. Repository op de server zetten (bijvoorbeeld `/opt/mijnummah`) en de gewenste versie uitchecken:
   `git checkout <tag>`.
4. Configuratie:
   ```sh
   cd /opt/mijnummah/deploy
   cp .env.example .env && chmod 600 .env
   openssl rand -base64 48   # voor JWT_SECRET
   openssl rand -base64 32   # voor POSTGRES_PASSWORD
   ```
   Vul `.env` in (zie de tabel hieronder). Bij `PUSH_PROVIDER=fcm`: zet het serviceaccountbestand in
   `deploy/secrets/fcm-service-account.json` met `chmod 600`.
5. Starten:
   ```sh
   IMAGE_TAG=$(git describe --tags --always) docker compose -f docker-compose.prod.yml up -d --build --wait
   ```
   Flyway voert bij de eerste start alle migraties uit. Met `BOOTSTRAP_ADMIN_EMAIL` en `BOOTSTRAP_ADMIN_PASSWORD`
   wordt de eerste platformbeheerder aangemaakt; haal het wachtwoord daarna uit `.env` en start opnieuw.
6. Reverse proxy instellen met HTTPS. Voorbeeld met Caddy (`/etc/caddy/Caddyfile`):
   ```
   api.example.nl {
       reverse_proxy 127.0.0.1:8080
   }
   beheer.example.nl {
       reverse_proxy 127.0.0.1:8081
   }
   ```
   Caddy haalt zelf certificaten op en zet `X-Forwarded-For`; de backend gebruikt die voor de limieten per IP
   (`FORWARD_HEADERS_STRATEGY=framework` staat al in de compose). Zet bij een andere proxy HSTS aan
   (`Strict-Transport-Security: max-age=31536000`).
7. Back-up inplannen (zie hieronder) en een eerste herstel testen.

## Configuratie

Alle instellingen staan in `deploy/.env` (voorbeeld: [`deploy/.env.example`](deploy/.env.example)). De backend
weigert te starten als een verplichte waarde ontbreekt of als een ontwikkelinstelling (`MAIL_PROVIDER=log`,
`PUSH_PROVIDER=log`) buiten het profiel `local` staat.

| Variabele | Verplicht | Uitleg |
|---|---|---|
| `POSTGRES_PASSWORD` | ja | Wachtwoord van de database |
| `JWT_SECRET` | ja | Ondertekent toegangstokens, minimaal 32 bytes. Wijzigen logt iedereen uit na hoogstens 15 minuten |
| `PORTAL_ORIGIN` | ja | Adres van het portaal (CORS), bijv. `https://beheer.example.nl` |
| `API_BASE_URL` | ja | Adres van de backend voor het portaal, bijv. `https://api.example.nl` |
| `MAIL_PROVIDER`, `MAIL_FROM`, `SPRING_MAIL_*` | voor wachtwoordherstel | `none` of `smtp`. Met `none` zegt de app eerlijk dat herstel niet beschikbaar is |
| `PUSH_PROVIDER`, `FCM_PROJECT_ID`, `FCM_CREDENTIALS_FILE` | voor pushmeldingen | `none` of `fcm` |
| `BOOTSTRAP_*` | alleen eerste start | Eerste platformbeheerder en eerste moskee |
| `IMAGE_TAG` | nee | Versie van de images; gebruikt bij rollback |
| `LOG_FORMAT` | nee | `ecs` (standaard), `logstash` of `gelf` |
| `RETENTION_*` | nee | Bewaartermijnen, zie `backend/.env.example` en `PRIVACY.md` |

## Geheimen

- Alleen in `deploy/.env` (rechten 600) en `deploy/secrets/` op de server. Beide staan in `.gitignore`.
- Bewaar een kopie in een wachtwoordkluis van de beheerder, niet in e-mail of chat.
- Roteren: nieuwe waarde in `.env`, `docker compose -f docker-compose.prod.yml up -d`. Voor het databasewachtwoord
  eerst in PostgreSQL wijzigen (`ALTER USER mijnummah PASSWORD '...'`), dan in `.env`.
- API-documentatie (Swagger) staat in productie uit (`API_DOCS_ENABLED` niet zetten).

## Back-up en herstel

**Back-up** (`deploy/backup.sh`): `pg_dump` in het custom-formaat naar `BACKUP_DIR` (standaard `deploy/backups`,
rechten 700), controleert dat de dump leesbaar is, en verwijdert back-ups ouder dan `BACKUP_KEEP_DAYS` (standaard 30).

```cron
15 2 * * *  cd /opt/mijnummah/deploy && ./backup.sh >> backups/backup.log 2>&1
```

Een back-up op dezelfde server beschermt niet tegen verlies van de server. Kopieer `backups/` dagelijks naar opslag
buiten de server (bijvoorbeeld met `restic` of `rclone` naar een S3-bucket in de EU, versleuteld), met dezelfde
bewaartermijn.

**Herstel testen of voorbereiden** (`deploy/restore.sh`): zet een back-up in een nieuwe, lege database naast de
live database. Het script weigert de live database en bestaande namen.

```sh
./restore.sh backups/mijnummah-20261011T021500Z.dump mijnummah_restore_20261011
```

**Overschakelen op een herstelde database** is een bewuste handmatige stap (gegevens na de back-up gaan verloren):

1. `docker compose -f docker-compose.prod.yml stop backend`
2. Controleer de herstelde database (aantallen, laatste migratie; het script toont `flyway_schema_history`).
3. Wissel de namen in PostgreSQL:
   ```sh
   docker compose -f docker-compose.prod.yml exec postgres psql -U mijnummah -d postgres \
     -c 'ALTER DATABASE mijnummah RENAME TO mijnummah_old_20261011' \
     -c 'ALTER DATABASE mijnummah_restore_20261011 RENAME TO mijnummah'
   ```
4. `docker compose -f docker-compose.prod.yml up -d backend --wait`
5. Laat de oude database staan tot zeker is dat alles werkt; verwijder hem daarna handmatig.

Test het herstel elk kwartaal en na elke grote wijziging.

## Logging en monitoring

- **Logs:** `docker compose -f docker-compose.prod.yml logs -f backend`. De backend logt JSON per regel (ECS), zonder
  persoonsgegevens (zie `PRIVACY.md`). Docker roteert per container op 5 × 10 MB. Wil je centraal verzamelen:
  Grafana Loki, Elastic of een vergelijkbare dienst kan de JSON direct lezen.
- **Reverse-proxylogs** bevatten IP-adressen: hoogstens 14 dagen bewaren.
- **Healthchecks:**

  | Adres | Betekenis |
  |---|---|
  | `https://api.<domein>/actuator/health` | `UP` als de backend draait en de database bereikbaar is |
  | `https://api.<domein>/actuator/health/readiness` | Klaar voor verkeer (gebruikt door de Docker-healthcheck) |
  | `https://api.<domein>/actuator/health/liveness` | Proces leeft |
  | `https://beheer.<domein>/healthz` | Portaal-nginx draait |

  Details staan uit (`show-details: never`). Een kapotte of ontbrekende SMTP-server maakt de backend niet `DOWN`;
  mislukte herstel-e-mails staan in het log.
- **Externe bewaking:** een uptime-dienst (bijvoorbeeld Uptime Kuma op een andere machine, of een dienst als
  UptimeRobot) die elke minuut de twee health-adressen controleert en mailt bij een storing. Bewaak ook de schijfruimte
  en of `backup.log` elke dag een nieuwe regel heeft.
- **Pushmeldingen die blijven hangen** worden na 30 minuten automatisch als mislukt gemarkeerd (D-041); zie het log
  `Push notification ... was still sending after PT30M; marked as failed`.

## Release

Een release is een git-tag op `main` nadat CI groen is (backend, portaal, app, design en de containerjob).

1. Tag: `git tag v0.6.0 && git push origin v0.6.0`.
2. Op de server: back-up maken (`./backup.sh`).
3. `git fetch --tags && git checkout v0.6.0`
4. `IMAGE_TAG=v0.6.0 docker compose -f docker-compose.prod.yml up -d --build --wait`
   (bewaar de tag ook in `.env` als `IMAGE_TAG=v0.6.0`).
5. Controleer health, log in op het portaal, open de app.

Flyway voert nieuwe migraties uit bij het starten van de backend. Migraties zijn alleen vooruit en worden nooit
achteraf gewijzigd.

## Rollback

- **Zonder nieuwe migratie** in de release: zet `IMAGE_TAG` terug op de vorige tag en
  `docker compose -f docker-compose.prod.yml up -d --wait`. De vorige images staan nog lokaal; anders
  `git checkout <vorige tag>` en `--build`.
- **Met een nieuwe migratie:** een oudere backend start niet op een nieuwer schema (Flyway valideert). Kies:
  1. bij voorkeur een **forward fix**: een nieuwe release die het probleem oplost, eventueel met een extra migratie;
  2. of **herstel van de back-up** van vóór de release (stappen hierboven) en daarna de vorige tag. Gegevens die na
     de back-up zijn ingevoerd gaan dan verloren.
- Noteer bij elke release in de PR of er een migratie in zit, zodat duidelijk is welke rollback kan.

## Wat is getest

| Wat | Resultaat (11 okt 2026) |
|---|---|
| Runtime-images backend en portaal gebouwd en als stack gestart (`docker compose ... up`) | Geslaagd lokaal: alle drie containers `healthy`, backend als uid 10001, logs als JSON zonder e-mailadressen |
| Inloggen in het portaal via de containers (Playwright) | Geslaagd lokaal: geen console- of CSP-fouten; nonce per verzoek, SPA-links werken |
| Portaal weigert te starten met een ongeldige `API_BASE_URL` | Geslaagd lokaal |
| `backup.sh` en `restore.sh` tegen de stack | Geslaagd lokaal: dump gemaakt en gecontroleerd, hersteld in een nieuwe database met dezelfde aantallen; live database en bestaande naam geweigerd |
| Volledige Docker-build (Maven en npm in de container) | Lokaal niet mogelijk: de build-container heeft hier geen internet. CI-job "Containers en productiestack" bouwt beide images, start de stack, doet een rooktest en een back-up en herstel |
| Echte server, TLS, DNS, externe back-upopslag, uptimebewaking | Niet getest: er is nog geen hosting |

## Productieblokkades

Wat nog moet gebeuren voordat de pilot live kan. Status per punt in `IMPLEMENTATION_PLAN.md`.

1. Hosting kiezen (EU), server inrichten, domeinen en DNS.
2. SMTP-dienst kiezen en instellen; herstel-e-mail end-to-end testen.
3. Firebase-project per app aanmaken, FCM-serviceaccount op de server, pushmeldingen testen op echte Android- en
   iOS-toestellen.
4. Apple Developer-account: capability Push Notifications, APNs-sleutel in Firebase, signing (fase 7).
5. Google Play- en App Store-accounts, store-teksten, schermafbeeldingen en de privacyformulieren.
6. Privacyverklaring op een vaste URL, verwerkersovereenkomsten, besluit over art. 9 AVG (zie `PRIVACY.md`).
7. Externe back-upopslag en uptimebewaking inrichten; eerste hersteltest op de echte server.
8. Release-builds van de app (APK/AAB en iOS) met de productie-`API_BASE_URL` en Firebase-instellingen.
