# Mijn Ummah backend

API voor de moskee-app (Flutter) en het beheerportaal (Angular). Eén modulaire monoliet bedient alle moskeeën
(tenants) vanuit één PostgreSQL-database; alle moskeespecifieke data hangt aan een verplichte `mosque_id`.

Java 21 · Spring Boot 4.1 · Spring MVC · Spring Security (JWT) · Spring Data JPA · Flyway · PostgreSQL 17 ·
Spring Modulith · springdoc-openapi · JUnit · Testcontainers · Maven

- [API.md](API.md): endpoints, authenticatie, autorisatie en foutcodes
- [DATABASE.md](DATABASE.md): tabellen, constraints en tenantisolatie
- Volledige specificatie: `/v3/api-docs` (OpenAPI 3) en `/swagger-ui.html` als de backend lokaal draait

## Lokaal draaien

Alleen gebruiken? Start alles in containers vanuit de root: `docker compose up -d --build` (zie de README in de root).
Om aan de backend te werken: alle commando's vanuit `backend/`. Vereist: JDK 21 en Docker (of een eigen PostgreSQL 16+).

```bash
cd backend
docker compose up -d                                 # PostgreSQL 17 op localhost:5434
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run  # API op http://localhost:8082
```

Het profiel `local` maakt bij de eerste start een platformbeheerder (`admin@mijnummah.local`, wachtwoord
`lokaal-wachtwoord-123`) en de moskee **Noeroel Islam** (slug `noeroel-islam`) aan, met die beheerder als ADMIN.
Dit zijn wegwerpgegevens voor een ontwikkelmachine; buiten `local` bestaan ze niet. In `local` staan e-mail en push op
`log`: herstelcodes en pushmeldingen komen in het log en worden **niet** verstuurd (het portaal toont dan
"Testomgeving").

```bash
curl -s localhost:8082/api/v1/public/mosques/noeroel-islam
curl -s -X POST localhost:8082/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@mijnummah.local","password":"lokaal-wachtwoord-123"}'
```

## Testen

```bash
./mvnw test     # unit tests en ModularityTest (modulegrenzen), zonder database
./mvnw verify   # plus integratietests (*IT) tegen echte PostgreSQL
```

De integratietests starten de volledige applicatie op een willekeurige poort en praten er via HTTP mee. Ze
gebruiken een PostgreSQL 17-Testcontainer (Docker nodig). Zonder Docker kan een bestaande, lege testdatabase
gebruikt worden; die wordt voor elke test leeggemaakt:

```bash
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/mijnummah_test \
TEST_DATABASE_USERNAME=mijnummah TEST_DATABASE_PASSWORD=mijnummah ./mvnw verify
```

## Configuratie

Alles via omgevingsvariabelen; er staan geen geheimen in de repository. Zie [.env.example](.env.example).

| Variabele | Verplicht | Betekenis |
|---|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | ja | PostgreSQL-verbinding |
| `JWT_SECRET` | ja | HS256-sleutel, minimaal 32 bytes (`openssl rand -base64 48`) |
| `CORS_ALLOWED_ORIGINS` | nee | Origins van het beheerportaal, kommagescheiden |
| `JWT_ACCESS_TOKEN_TTL`, `REFRESH_TOKEN_TTL` | nee | Standaard 15 minuten en 30 dagen |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | nee | Eerste platformbeheerder, alleen als er nog geen is |
| `BOOTSTRAP_MOSQUE_SLUG`, `BOOTSTRAP_MOSQUE_NAME`, `BOOTSTRAP_MOSQUE_ADMIN_EMAIL` | nee | Eerste moskee en haar ADMIN |
| `API_DOCS_ENABLED` | nee | OpenAPI en Swagger UI; standaard uit buiten `local` |
| `FORWARD_HEADERS_STRATEGY` | nee | `framework` achter een vertrouwde reverse proxy |
| `MAIL_PROVIDER` | nee | `none` (standaard: wachtwoordherstel geeft `503`), `smtp`, of `log` (alleen lokaal: e-mail naar het log) |
| `MAIL_FROM` | bij `smtp` | Afzender van de herstel-e-mail, bijvoorbeeld `Noeroel Islam <noreply@example.org>` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | bij `smtp` | SMTP-server; voor STARTTLS ook `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true` |
| `PUSH_PROVIDER` | nee | `none` (standaard: verzenden geeft `503`), `fcm`, of `log` (alleen lokaal: meldingen naar het log) |
| `FCM_PROJECT_ID` | bij `fcm` | Project-ID van het Firebase-project |
| `FCM_CREDENTIALS_FILE` | bij `fcm` | Pad naar het JSON-sleutelbestand van een serviceaccount met de rol "Firebase Cloud Messaging API Admin"; buiten de repository bewaren |
| `RETENTION_ENABLED`, `RETENTION_CRON` | nee | Nachtelijke opschoning, standaard aan om 03:30 (Europe/Amsterdam) |
| `RETENTION_EXPIRED_SESSIONS`, `RETENTION_EXPIRED_RESET_CODES`, `RETENTION_EVENT_REGISTRATIONS`, `RETENTION_INACTIVE_PUSH_DEVICES` | nee | Bewaartermijnen als ISO-8601-duur, standaard `P7D`, `P1D`, `P365D`, `P270D`; uitleg in [PRIVACY.md](../PRIVACY.md) |
| `RETENTION_PUSH_SENDING_TIMEOUT` | nee | Na deze tijd (standaard `PT30M`) wordt een melding die nog "wordt verstuurd" als mislukt gemarkeerd |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | nee | `ecs` voor JSON-logs per regel (zo staat het in de productie-compose) |

Productie (Docker-image, compose, back-ups): zie [DEPLOYMENT.md](../DEPLOYMENT.md).

## Structuur

Elke module is een directe subpackage van `nl.mijnummah` met de lagen `api` (controllers, request- en
response-records), `application` (use cases, transacties, autorisatie), `domain` (entiteiten en regels) en
`infrastructure` (repositories). Andere modules gebruiken alleen de typen in de basispackage van een module.
`ModularityTest` laat de build falen bij overtredingen of cyclische afhankelijkheden.

| Module | Inhoud |
|---|---|
| `common` | Foutmodel, klok, rate limiting, paginering, OpenAPI-metadata (geen domeinbegrippen) |
| `identity` | Accounts, platformrollen, inloggen, refresh tokens, HTTP-beveiliging |
| `mosque` | Moskeeën, beheerders per moskee, `MosqueAccessService` |
| `event` | Agenda en aanmeldingen |
| `media` | Lezingen en video's via de `VideoProvider`-interface (YouTube) |
| `announcement` | Aankondigingen |
| `follow` | Moskeeën volgen en meldingsvoorkeuren; `FollowerDirectory` voor de pushdoelgroep |
| `notification` | Pushtoestellen, pushmeldingen per content, FCM HTTP v1 (D-032 t/m D-034, D-038) |

Afhankelijkheden: `mosque → identity`; `event`, `announcement`, `follow → mosque, identity`; `media → event, mosque,
identity`; `notification → follow, mosque, identity`; `announcement`, `event` en `media` gebruiken
`notification` voor de pushknop.
