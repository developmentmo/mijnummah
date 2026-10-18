# Mijn Ummah

Platform voor moskee-apps. De eerste pilot is de app van **Noeroel Islam**: een mobiele app (Flutter),
een beheerportaal (Angular) en een gedeelde backend (Spring Boot, PostgreSQL). Elke moskee krijgt een eigen app uit
dezelfde code: zie [WHITE_LABEL.md](WHITE_LABEL.md).

| Map | Inhoud |
|---|---|
| [`backend/`](backend/README.md) | Spring Boot-API en PostgreSQL-migraties |
| `frontend/app/` | Flutter-app |
| `frontend/webportaal/` | Angular-beheerportaal |
| [`brands/`](WHITE_LABEL.md) | Merk per moskee-app: naam, ID's, kleuren, beeldmerk |
| [`tools/brand/`](WHITE_LABEL.md#commandos) | Merken controleren, aanmaken en toepassen |
| [`design/`](design/README.md) | Design tokens, fonts, logo's en iconen |
| [`deploy/`](DEPLOYMENT.md) | Productie-compose, back-up en herstel |

## Lokaal starten

Alles in één keer (vereist Docker): database, backend en beheerportaal, gebouwd uit deze code.

```bash
docker compose up -d --build
```

Portaal op http://localhost:4200 (inloggen als `admin@mijnummah.local`, wachtwoord `lokaal-wachtwoord-123`), API op
http://localhost:8082. Na codewijzigingen opnieuw `docker compose up -d --build`; stoppen met `docker compose down`.
Wie aan de backend of het portaal ontwikkelt, start alleen de database (`docker compose up -d postgres`) en draait de
rest zoals in de README van [backend](backend/README.md) en [portaal](frontend/webportaal/README.md).

De repository is nog in opbouw. Zie [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) voor de fases en de
voortgang, en [DECISIONS.md](DECISIONS.md) voor de architectuur- en ontwerpbeslissingen. Privacy en AVG:
[PRIVACY.md](PRIVACY.md). Productie: [DEPLOYMENT.md](DEPLOYMENT.md). Latere uitbreidingen (lidmaatschap,
live-streaming, betalingen): [EXTENSIONS.md](EXTENSIONS.md).
