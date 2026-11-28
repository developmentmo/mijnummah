# Livegang: stappenplan

Hoe Mijn Ummah van "klaar in de repository" naar live gaat: eerst de server met API en beheerportaal, daarna de app
in de stores. Elke stap noemt wat je nodig hebt. Details staan in [DEPLOYMENT.md](DEPLOYMENT.md) en
[store/README.md](store/README.md).

Niets hieronder gebeurt automatisch: de server, de domeinen en de store-accounts beheer je zelf, en elke stap die
iets live zet doe je bewust.

## 1. Kiezen en regelen

| Wat | Advies | Waarom |
|---|---|---|
| Server (VPS) | Een kleine VPS in de EU met 2 vCPU, 4 GB geheugen en 40 GB schijf, met Ubuntu 24.04 LTS (bijvoorbeeld Hetzner in Duitsland of Finland) | Genoeg voor de pilot; gegevens blijven in de EU |
| Domein | Bijvoorbeeld `mijnummah.nl` met `api.` en `beheer.` als subdomeinen | Adressen voor de app en het portaal |
| E-maildienst (SMTP) | Een dienst met servers in de EU en een verwerkersovereenkomst (bijvoorbeeld Brevo, Mailjet of Scaleway TEM) | Uitnodigingen en wachtwoordherstel |
| Externe back-upopslag | S3-opslag in de EU (bijvoorbeeld Hetzner Object Storage, Scaleway of Backblaze B2 EU) | Een back-up op dezelfde server is geen back-up |
| Bewaking | Een gratis account bij healthchecks.io (of een eigen Uptime Kuma op een andere machine) | Een melding als de server of de back-up stopt |
| Firebase-project | Eén project met een Android- en een iOS-app `nl.mijnummah.app` | Pushmeldingen |
| Apple Developer Program | Op naam van de organisatie (vraagt een D-U-N-S-nummer) | iOS-build en App Store |
| Google Play Console | Organisatieaccount | Android en Google Play |
| Privacyverklaring | Concept staat op `/privacy`; laten nakijken en de punten tussen [haken] invullen | Verplicht voor beide stores en de AVG |

Leg alle wachtwoorden en sleutels vast in een wachtwoordkluis, niet in e-mail of chat.

## 2. Server inrichten

1. Ubuntu 24.04 installeren; inloggen met een SSH-sleutel, wachtwoordlogin uit.
2. Docker Engine met de Compose-plugin installeren (docs.docker.com, "Install Docker Engine on Ubuntu").
3. Firewall: alleen 22, 80 en 443 open (`ufw allow OpenSSH`, `ufw allow 80,443/tcp`, `ufw allow 443/udp`,
   `ufw enable`). Let op: Docker publiceert poorten buiten ufw om; de stack publiceert daarom alleen 80 en 443
   (Caddy), de rest staat op `127.0.0.1`.
4. Automatische beveiligingsupdates aan (`unattended-upgrades`).
5. DNS: A-records (en AAAA bij IPv6) voor `api.<domein>` en `beheer.<domein>` naar de server.

## 3. Configureren en starten

```sh
sudo git clone https://github.com/developmentmo/mijnummah.git /opt/mijnummah
cd /opt/mijnummah/deploy
cp .env.example .env && chmod 600 .env
openssl rand -base64 32   # POSTGRES_PASSWORD
openssl rand -base64 48   # JWT_SECRET
```

Is de repository privé, maak dan op de server een SSH-sleutel en voeg die in GitHub toe als deploy key met alleen
leesrechten; clone dan via `git@github.com:developmentmo/mijnummah.git`.

Vul `.env` in: domeinen, `ACME_EMAIL`, SMTP, en voor de eerste start `BOOTSTRAP_ADMIN_EMAIL` en
`BOOTSTRAP_ADMIN_PASSWORD`. Push kan in eerste instantie op `none` blijven tot Firebase klaar is. Controleer met:

```sh
./preflight.sh
```

Zet daarna de eerste versie live (de tag maakt iemand met schrijfrechten op `main`, na groene CI):

```sh
./deploy.sh v1.0.0
```

Het script maakt een back-up (vanaf de tweede keer), bouwt en start de stack, wacht tot alles gezond is en test de
API en het portaal via HTTPS. Bij de eerste start haalt Caddy zelf de certificaten op.

Daarna: inloggen op `https://beheer.<domein>` als platformbeheerder, `BOOTSTRAP_ADMIN_PASSWORD` uit `.env` halen en
`./preflight.sh` nog eens draaien.

## 4. Back-up en bewaking

```sh
sudo crontab -e
```

```cron
15 2 * * *   cd /opt/mijnummah/deploy && ./backup.sh >> backups/backup.log 2>&1 && ./offsite-backup.sh >> backups/backup.log 2>&1
*/5 * * * *  cd /opt/mijnummah/deploy && ./check.sh >> backups/check.log 2>&1
```

- Maak bij healthchecks.io een check met periode 5 minuten en zet de ping-URL in `HEALTHCHECK_PING_URL`. Je krijgt
  een e-mail als een controle faalt of als de server niet meer pingt.
- Test het herstel één keer: `./offsite-backup.sh restore /tmp/hersteltest`, dan
  `./restore.sh /tmp/hersteltest/backups/<dump> mijnummah_hersteltest`. Verwijder die database daarna.

## 5. Eerste moskee

Volg [TESTHANDLEIDING.md](TESTHANDLEIDING.md) vanaf stap 2, nu op de productieomgeving: de platformbeheerder
registreert de moskee met haar beheerder, die beheerder krijgt een uitnodiging per e-mail en richt de moskee in.
Gebruik op productie geen testmoskeeën uit `tools/demo/`.

## 6. Pushmeldingen

1. In Firebase: een serviceaccount-sleutel maken en op de server zetten als `deploy/secrets/fcm-service-account.json`
   (`sudo chown 10001:10001` en `chmod 600`; de backend draait als uid 10001).
2. In `.env`: `PUSH_PROVIDER=fcm` en `FCM_PROJECT_ID`, dan `./preflight.sh` en
   `docker compose -f docker-compose.prod.yml up -d --wait`.
3. Voor iOS: in het Apple Developer-account een APNs-sleutel maken en die in Firebase toevoegen.

## 7. App bouwen en in de stores zetten

1. GitHub Environment `productie` vullen: `API_BASE_URL=https://api.<domein>`, de Firebase-instellingen en de
   Android-uploadsleutel (zie [DEPLOYMENT.md](DEPLOYMENT.md#release-van-de-app)).
2. Privacyverklaring vaststellen (zie [store/README.md](store/README.md)) en het adres in de app zetten.
3. Actions > Release-app > Run workflow, voor Android en iOS.
4. Testen op echte toestellen tegen productie: inloggen, een moskee volgen, een pushmelding ontvangen, aanmelden.
5. Play Console: interne test, daarna productie. App Store Connect: TestFlight, daarna ter beoordeling. Teksten en
   formulieren staan in [store/](store/).

## Na de livegang

- Nieuwe versie: tag op `main`, dan `./deploy.sh <tag>` op de server.
- Elk kwartaal een hersteltest; `check.log` en de e-mails van healthchecks.io in de gaten houden.
- Terug naar de vorige versie: zie [DEPLOYMENT.md](DEPLOYMENT.md#rollback).
