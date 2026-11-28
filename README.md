# Mijn Ummah

Eén islamitisch communityplatform voor alle aangesloten moskeeën: **één mobiele app** (Flutter) waarin gebruikers
moskeeën ontdekken, volgen en hun activiteiten, lezingen en nieuws zien, een **beheerportaal** (Angular) voor de
platformbeheerder en voor elke moskee een eigen afgeschermde beheeromgeving, en een gedeelde backend (Spring Boot,
PostgreSQL). Elke moskee is een tenant; een nieuwe moskee toevoegen is configuratie, geen nieuwe app.

| Map | Inhoud |
|---|---|
| [`backend/`](backend/README.md) | Spring Boot-API en PostgreSQL-migraties |
| `frontend/app/` | Flutter-app |
| `frontend/webportaal/` | Angular-beheerportaal |
| `brands/mijnummah/` | Het merk van de app: naam, ID's, kleuren, beeldmerk ([huisstijl](BRAND_GUIDELINES.md)) |
| `tools/brand/` | Het merk controleren en toepassen op app en portaal |
| [`design/`](design/README.md) | Design tokens, fonts, logo's en iconen |
| [`deploy/`](DEPLOYMENT.md) | Productie-compose met Caddy, release, back-up, herstel en bewaking |
| [`store/`](store/README.md) | Teksten en formulieren voor Google Play en de App Store |

## Lokaal starten

Alles in één keer (vereist Docker): database, backend en beheerportaal, gebouwd uit deze code.

```bash
docker compose up -d --build
```

Portaal op http://localhost:4200 (inloggen als `admin@mijnummah.local`, wachtwoord `lokaal-wachtwoord-123`), API op
http://localhost:8082. Er staan nog geen moskeeën klaar: registreer er een in het portaal, of zet testmoskeeën klaar
met [tools/demo](tools/demo/README.md). Een pilot doorlopen: [TESTHANDLEIDING.md](TESTHANDLEIDING.md). Na codewijzigingen opnieuw `docker compose up -d --build`; stoppen met `docker compose down`.
Wie aan de backend of het portaal ontwikkelt, start alleen de database (`docker compose up -d postgres`) en draait de
rest zoals in de README van [backend](backend/README.md) en [portaal](frontend/webportaal/README.md).

De repository is nog in opbouw. Zie [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) voor de fases en de
voortgang, en [DECISIONS.md](DECISIONS.md) voor de architectuur- en ontwerpbeslissingen. Privacy en AVG:
[PRIVACY.md](PRIVACY.md). Productie: [DEPLOYMENT.md](DEPLOYMENT.md); livegang stap voor stap: [GOLIVE.md](GOLIVE.md). Latere uitbreidingen (lidmaatschap,
live-streaming, betalingen): [EXTENSIONS.md](EXTENSIONS.md).
