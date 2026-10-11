# Implementatieplan — Noeroel Islam (platform Mijn Ummah)

Dit document beschrijft alle fases, hun acceptatiecriteria, afhankelijkheden en de voortgang.
Beslissingen en hun onderbouwing staan in [DECISIONS.md](DECISIONS.md).

Statuslabels: **Nog niet begonnen** · **Bezig** · **Geïmplementeerd** · **Getest en geslaagd** ·
**Gedeeltelijk geïmplementeerd** · **Geblokkeerd**. Een fase is pas voltooid volgens de Definition of Done
uit het masterprompt (code, tests, build, regressies, documentatie, beperkingen, samenvatting).

## Voortgang

| Fase | Onderwerp | Status |
|---|---|---|
| 0 | Repositoryanalyse en technisch plan | Voltooid en goedgekeurd |
| 1 | Branding en design system | Voltooid en goedgekeurd (PR #3 gemerged) |
| 3 | Backend, database en API (vóór fase 2, zie D-004) | Getest en geslaagd (lokaal en in CI); wacht op beoordeling |
| 2 | UX/UI en Flutter-app | Nog niet begonnen |
| 4 | Beheerportaal (Angular) | Nog niet begonnen |
| 5 | Accounts, volgen en notificaties | Nog niet begonnen |
| 6 | Testen, privacy, beveiliging, productievoorbereiding | Nog niet begonnen |
| 7 | Voorbereiding white-label automatisering | Nog niet begonnen |
| 8 | Latere uitbreidingen (niet implementeren) | Alleen documentatie |

---

## Fase 0 — Repositoryanalyse

### Bevinding: de repository is leeg

Geïnspecteerd op 11 oktober 2026: `developmentmo/mijnummah` bevatte **geen enkele commit en geen bestanden**.
Er is dus geen bestaande Flutter-app, Spring Boot-backend, Angular-portaal, PostgreSQL-configuratie,
migraties, authenticatie, API, tests, buildconfiguratie, Docker/CI of designassets om op voort te bouwen.

Om een draft-PR mogelijk te maken is op `main` één initiële commit geplaatst met alleen `README.md` en
`.gitignore`. Verder is er niets aan de repository gewijzigd.

Het masterprompt gaat uit van een bestaande codebase ("werk met de bestaande backend", "de bestaande
Angular-applicatie"). Die aanname klopt niet. Alle fases bouwen daarom nieuw op, en waar het masterprompt
"bestaande conventies" vraagt, gebruiken we de conventies uit de eigen projecten van dezelfde eigenaar
(`developmentmo/repairtrack`, en `mariambm/maktab` als tweede referentie). Zie D-002.

### Huidige implementatie tegenover de gewenste MVP

| MVP-onderdeel | Huidig | Benodigd |
|---|---|---|
| Huisstijl, logo, app-icoon | Niets | Volledig nieuw (fase 1) |
| Design tokens Flutter en Angular | Niets | Eén bron, gegenereerd naar beide (fase 1) |
| Flutter-app met vijf tabs | Niets | Nieuw project (scaffold fase 1, schermen fase 2) |
| Agenda, evenementen, aanmeldingen | Niets | Backend fase 3, app fase 2, beheer fase 4 |
| Lezingen en videobibliotheek | Niets | Mediamodule met video-providerinterface |
| Aankondigingen | Niets | Backend, app, beheer |
| Accounts, volgen, pushmeldingen | Niets | Fase 5, FCM/APNs vereist externe accounts |
| Beheerportaal | Niets | Nieuw Angular-project (scaffold fase 1, schermen fase 4) |
| Tenantisolatie (`mosque_id`) | Niets | Vanaf de eerste migratie in het datamodel |
| Docker, CI/CD, deployment | Niets | CI vanaf fase 1, deployment in fase 6 |

### Beoogde repositorystructuur

```text
mijnummah/
├── backend/           Spring Boot 4 (Java 21, Maven), modulaire monoliet, Flyway, PostgreSQL 17
├── frontend/
│   ├── app/           Flutter-app (Riverpod, Dio, go_router)
│   └── webportaal/    Angular-beheerportaal (Angular Material, M3-thema op design tokens)
├── design/
│   ├── tokens/        tokens.json (enige bron) + generator naar Dart en CSS
│   └── branding/      SVG-bronbestanden van logo's en app-icoon, plus exports
├── deploy/            Docker Compose en reverse proxy (fase 6)
├── docs/              Architectuur, API, database, privacy
└── .github/workflows/ CI per onderdeel
```

### Technische risico's

| # | Risico | Gevolg | Maatregel |
|---|---|---|---|
| R1 | Repo is leeg terwijl het plan uitgaat van bestaande code | Meer werk per fase dan voorzien | Scaffolding expliciet in fase 1 (app/portaal) en fase 3 (backend) opgenomen |
| R2 | Fasevolgorde: Flutter-schermen (fase 2) vóór de backend (fase 3) | Schermen zonder echte API leiden tot mockdata, wat de regels verbieden | Besloten: fase 3 vóór fase 2 (D-004) |
| R3 | Goud `#C6A45A` haalt op licht geen WCAG-contrast (2,14 op ivoor, 2,37 op wit) | Onleesbare tekst/iconen | Goud alleen decoratief op licht of als tekst op donkergroen (5,16). Extra donkere goudtint voor tekst op licht. Contrasttest in CI (D-008) |
| R4 | Jadegroen op ivoor is 4,69:1, net boven AA | Kleine tekst of afwijkende tinten zakken eronder | Jade alleen voor normale/grote tekst en knoppen met witte tekst (5,19); automatische contrastcontrole |
| R5 | Tenantisolatie later inbouwen is duur en foutgevoelig | Datalekken tussen moskeeën | `mosque_id` + autorisatieservice vanaf de eerste migratie; isolatietests (D-006) |
| R6 | Videoplatform nog niet gekozen | Mediamodule kan niet volledig getest worden met echte video's | Providerinterface met YouTube-adapter als standaard; geen video in PostgreSQL (D-009) |
| R7 | Pushmeldingen vereisen Firebase-project en Apple Developer-account | Fase 5 deels geblokkeerd | Configuratie en lokale tests bouwen; echte verzending pas met credentials |
| R8 | Flutter SDK staat niet in deze cloudomgeving | Builds/tests in fase 1 vereisen installatie | Flutter stable (3.47.7) is downloadbaar; installatie per sessie of via setup-script. iOS-builds kunnen hier niet (geen macOS), alleen in CI op een macOS-runner of lokaal |
| R9 | Google Fonts is vanuit deze omgeving niet bereikbaar | Fonts ophalen kost een alternatieve route | Fonts via npm (`@fontsource`) of de officiële repo's binnenhalen en lokaal bundelen (ook beter voor privacy, D-007) |
| R10 | Geen Docker-daemon in deze omgeving | Testcontainers-integratietests draaien hier niet | Integratietests in GitHub Actions (Docker beschikbaar); lokaal een PostgreSQL-service waar mogelijk |
| R11 | Rechten op beelden en opnames van lezingen | AVG- en auteursrechtrisico | Alleen placeholders die duidelijk als voorbeeld gemarkeerd zijn; publicatierechten vastleggen in fase 6 |
| R12 | Naam "Mijn Ummah" (repo/platform) tegenover "Noeroel Islam" (app) | Verwarring in naamgeving van packages en bundle-ID's | Platformcode generiek (`mijnummah`), merkspecifieke waarden alleen in brandconfiguratie (D-003) |

### Ontbrekende informatie

**Blokkerend voor fase 1:** niets. Fase 1 kan starten zodra je dit plan goedkeurt.

**Blokkerend voor latere fases** (nu niet nodig, wel tijdig aanleveren):

| Nodig vanaf | Informatie |
|---|---|
| Fase 2 | Echte gegevens van Noeroel Islam voor Ontdekken: adres, contact, korte beschrijving, vaste activiteiten. Zonder deze gegevens komen er gemarkeerde placeholders |
| Fase 3 | Keuze videoplatform (standaard: YouTube, ongepubliceerde of openbare video's) |
| Fase 5 | Firebase-project en Apple Developer-account (APNs-sleutel) |
| Fase 5 | E-mailprovider voor wachtwoordherstel (bijv. SMTP of een transactionele dienst) |
| Fase 6 | Hosting (standaard: een VPS met Docker Compose, zoals bij RepairTrack), domeinnaam |
| Fase 7 | Definitieve bundle identifier en Android application ID (voorstel: `nl.mijnummah.noeroelislam`) |

---

## Fase 1 — Branding en design system

**Afhankelijkheden:** goedkeuring van fase 0. Geen externe accounts nodig.

### Aanpak

1. **Kleurenpalet.** De zes basiskleuren worden uitgebreid tot tintenreeksen (50–900) voor groen, jade,
   ivoor/neutraal en goud, plus semantische kleuren (succes, waarschuwing, fout, info). Daarop komt een
   semantische laag (`surface`, `onSurface`, `primary`, `border`, `focus` enzovoort) voor licht en donker.
   Goud blijft accent: dunne lijnen, iconaccenten, highlights op donkergroen. Voor tekst op licht komt een
   donkere goudtint die AA haalt.
2. **Logo.** Handgemaakte SVG's: een minimalistische boog (mihrab) met daarin een lichtpunt of opengaande
   lijn als symbool voor licht en kennis ("Noer" betekent licht). Varianten: primair horizontaal, compact
   (alleen beeldmerk), app-icoon (vol vlak, ook als Android adaptive foreground/background), licht, donker
   en monochroom. De woordmark wordt naar paden omgezet, zodat er geen afhankelijkheid van geïnstalleerde
   fonts is.
3. **Typografie.** Fraunces (koppen) met Inter (tekst en UI), beide open source onder de SIL Open Font
   License 1.1. Noto Naskh Arabic voor de enkele Arabische tekst. Alle fonts worden lokaal gebundeld, er
   worden geen fonts tijdens gebruik van externe servers geladen (D-007).
4. **Design tokens.** Eén bronbestand `design/tokens/tokens.json` (W3C Design Tokens-formaat) met kleuren,
   typografie, spacing, radius, schaduw, iconmaten, motion en componenttokens (knop, invoerveld, card,
   navigatie, modal, melding). Een klein Node-script zonder externe dependencies genereert:
   - `frontend/app/lib/core/design/tokens.g.dart` (constanten),
   - `frontend/webportaal/src/styles/tokens.generated.css` (CSS custom properties).
   Op beide gaat een handgeschreven themalaag: `ThemeData` + `ThemeExtension` in Flutter en een Angular
   Material M3-thema in het portaal (D-005).
5. **Scaffolding** (nodig omdat de repo leeg is): een minimale Flutter-app in `frontend/app/` en een minimale
   Angular-app in `frontend/webportaal/`, elk met alleen het thema en een interne designpreview van tokens en
   basiscomponenten. Nog geen functionele schermen; die horen bij fase 2 en 4.
6. **Beeldtaal.** Richtlijnen voor fotografie en illustraties, plus neutrale placeholder-afbeeldingen die
   zichtbaar als "Voorbeeld" gemarkeerd zijn. Geen stockfoto's van personen die betrokkenheid bij Noeroel
   Islam suggereren.
7. **`BRAND_GUIDELINES.md`** met kleurcodes en contrastwaarden, typografie, logogebruik (vrije ruimte,
   minimale maat, verboden toepassingen), componentregels en toegankelijkheidsrichtlijnen.

### Opgeleverde bestanden

```text
BRAND_GUIDELINES.md                               huisstijlrichtlijnen
design/tokens/tokens.json                         enige bron van alle tokens (W3C Design Tokens-formaat)
design/tokens/contrast-pairs.json                 kleurparen die WCAG AA moeten halen
design/tokens/build.mjs, lib.mjs                  generator Dart + CSS, met --check modus
design/tokens/contrast.mjs                        WCAG-contrastcontrole
design/tokens/check-hardcoded.mjs                 zoekt losse kleuren en fonts buiten de tokens
design/fonts/                                     variabele bronfonts + OFL-licenties, build_fonts.py
design/branding/build_branding.py                 genereert alle logo's en app-iconen
design/branding/check_assets.py                   controleert SVG's, iconmaten en kopieën
design/branding/svg/, png/                        15 SVG-varianten, store-iconen, logo-exports
frontend/app/                                     Flutter-project (Android en iOS), app-ID nl.mijnummah.noeroelislam
frontend/app/lib/core/design/                     tokens.g.dart (gegenereerd, incl. AppColors ThemeExtension),
                                                  app_theme.dart, brand_logo.dart, components/ (melding,
                                                  skeleton, lege/fouttoestand), design.dart
frontend/app/lib/features/design_preview/         interne designpreview (startscherm in fase 1)
frontend/app/assets/branding/, assets/fonts/      gegenereerde logo's en statische fonts
frontend/app/android/.../res/mipmap-*/            launcher-iconen, adaptive en themed icon
frontend/app/ios/.../AppIcon.appiconset/          iOS-icoon 1024x1024 zonder alfakanaal
frontend/app/test/                                thema-, logo- en previewtests
frontend/webportaal/                              Angular 22-project met Angular Material en ESLint
frontend/webportaal/src/styles/                   tokens.generated.css, fonts.css, _material-theme.scss, _base.scss
frontend/webportaal/src/app/core/theme/           licht/donker/systeem
frontend/webportaal/src/app/shared/ui/            logo, melding, skeleton, lege toestand
frontend/webportaal/src/app/design-preview/       interne designpreview op /
frontend/webportaal/public/                       favicon, apple-touch-icon, logo's, WOFF2-fonts
.github/workflows/ci.yml                          design-, Flutter- en portaalcontroles
```

### Tests en controles

| Controle | Commando | Resultaat (11 okt 2026, lokaal en in CI) |
|---|---|---|
| Gegenereerde tokens zijn actueel | `node design/tokens/build.mjs --check` | Geslaagd |
| WCAG 2.2 AA voor alle kleurparen | `node design/tokens/contrast.mjs` | Geslaagd, 107 van 107 paren |
| Geen losse kleuren/fonts buiten de tokens | `node design/tokens/check-hardcoded.mjs` | Geslaagd |
| SVG's geldig, iconmaten, iOS-icoon zonder alfa, kopieën gelijk aan bron | `python3 design/branding/check_assets.py` | Geslaagd, 15 bron-SVG's en 10 kopieën |
| Flutter-formattering en analyse | `dart format …`, `flutter analyze` | Geslaagd, geen meldingen |
| Flutter-tests (thema, SVG's laden in flutter_svg, licht/donker, telefoon, tablet, 200% tekst, semantics, minder beweging) | `flutter test` | Geslaagd, 22 tests |
| Portaal formattering, lint, tests, build | `npm run format:check`, `npm run lint`, `npm test`, `npm run build` | Geslaagd, 13 tests |
| Flutter Android-build | `flutter build apk --debug` | Geslaagd in CI (GitHub Actions); lokaal niet mogelijk omdat de Android SDK hier niet te downloaden is |
| iOS-build | — | Niet uitgevoerd: vereist macOS en Xcode |

Afwijking van het plan: geen golden tests. Fontrendering verschilt per besturingssysteem, waardoor goldens
tussen een Mac en CI onbetrouwbaar worden (D-015). In plaats daarvan testen de widgettests op overflow bij
verschillende schermformaten en tekstschalen, en zijn de previews handmatig visueel gecontroleerd.

### Acceptatiecriteria fase 1

- Compleet palet met licht- en donkerthema; alle tekstcombinaties gecontroleerd op WCAG AA.
- Geldige SVG-logo's in alle genoemde varianten en app-iconen in de juiste rastermaten.
- Eén tokenbron die aantoonbaar zowel Flutter als Angular voedt; CI faalt als ze uit de pas lopen.
- `BRAND_GUIDELINES.md` aanwezig en consistent met de tokens.
- Geen hardcoded kleuren, fonts of afmetingen buiten de design-laag.
- Flutter- en Angular-scaffold bouwen en testen groen in CI.

---

## Fase 2 — UX/UI en Flutter-app

**Afhankelijkheden:** fase 1 en fase 3 (fase 3 wordt eerst uitgevoerd, D-004).

- Navigatie met vijf tabs (Home, Agenda, Lezingen, Ontdekken, Profiel) via `go_router` met `StatefulShellRoute`.
- Home, Agenda + detail, Lezingen + zoeken/categorieën + detail met videospeler, Ontdekken, Profiel.
- Riverpod voor state, Dio met een gegenereerde of handgeschreven API-client op basis van de OpenAPI-spec.
- Toestanden voor laden (skeletons), leeg, fout met opnieuw proberen, offline (cache van laatst geladen data).
- Datum/tijd in het Nederlands en `Europe/Amsterdam` (`intl` + `timezone`).
- Toegankelijkheid: `Semantics`-labels, tekstschaal tot 200%, focusstatussen, contrast.

**Acceptatiecriteria:** alle geïmplementeerde schermen zijn doorloopbaar tegen de echte backend; widgettests
voor kernschermen en toestanden; geen dode knoppen of mockdata in productieflows; Android-build groen.

## Fase 3 — Backend, database en API

**Afhankelijkheden:** fase 0. Geen externe accounts.

### Aanpak

- Spring Boot 4.1, Java 21, Maven, Spring Modulith (modulaire monoliet), Flyway, PostgreSQL 17, springdoc OpenAPI.
- Modules: `common`, `identity` (accounts, platformrollen, authenticatie), `mosque` (tenant, beheerders per moskee,
  `MosqueAccessService`), `event` (+ aanmeldingen), `media`, `announcement`, `follow` (volgen en
  meldingsvoorkeuren, D-018).
- Tabellen met UUID-sleutels, `mosque_id` als verplichte foreign key op alle moskeespecifieke data, indexen,
  unieke constraint tegen dubbele aanmeldingen, publicatiestatus, zichtbaarheid en audit-kolommen.
- Publieke endpoints (alleen gepubliceerde content van actieve moskeeën) en beheerendpoints met server-side
  autorisatie via `MosqueAccessService`; de moskee komt uit de route en wordt gecontroleerd tegen de rechten van
  de gebruiker (D-017).
- DTO's als Java records, uniforme foutrespons met stabiele codes (D-016), Bean Validation.
- Video's alleen als verwijzing via de `VideoProvider`-interface, YouTube eerst (D-009, D-021). Geen livestreaming.

### Opgeleverd

```text
backend/
  pom.xml, mvnw, docker-compose.yml (PostgreSQL 17), .env.example
  README.md, API.md, DATABASE.md
  src/main/java/nl/mijnummah/
    common/        foutmodel, klok (microseconden), rate limiter, paginering, OpenAPI-metadata
    identity/      accounts, JWT + roterende refresh tokens, login-throttle, beveiligingsconfiguratie, bootstrap
    mosque/        moskeeën, ADMIN/EDITOR per moskee, MosqueAccessService, MosqueDirectory, bootstrap
    event/         agenda, levenscyclus (concept, gepubliceerd, geannuleerd), aanmeldingen met capaciteit
    media/         media-items, VideoProvider + YouTube-adapter, zoeken, gerelateerde items
    announcement/  aankondigingen met vervaldatum en uitgelicht
    follow/        volgen, meldingsvoorkeuren per onderwerp
  src/main/resources/db/migration/V1..V9  (10 tabellen)
  src/test/java/…  9 unit-testklassen, ModularityTest, 9 integratietestklassen
.github/workflows/ci.yml  nieuwe job "Backend" (./mvnw verify met Testcontainers)
```

58 endpoints: 9 publiek, 3 voor inloggen/verversen/uitloggen, 9 voor ingelogde gebruikers en 37 voor beheer. Overzicht in `backend/API.md`.

### Tests en controles

| Controle | Commando | Resultaat (11 okt 2026) |
|---|---|---|
| Unit tests (YouTube-parser, rechtenmatrix, MosqueAccessService, levenscyclus activiteit en aankondiging, rate limiter, paginering, foutmapping) | `./mvnw test` | Geslaagd lokaal, 49 tests |
| Modulegrenzen en geen cycli | `ModularityTest` (in `./mvnw test`) | Geslaagd lokaal |
| Integratietests: authenticatie (rotatie, hergebruik, blokkeren, throttle), moskeebeheer, tenantisolatie A/B (30 pogingen), agenda en aanmeldingen (duplicaat, capaciteit, regels), media, aankondigingen, volgen, databaseconstraints, OpenAPI | `./mvnw verify` met `TEST_DATABASE_URL` | Geslaagd lokaal tegen PostgreSQL 16, 55 tests |
| Migraties reproduceerbaar op een lege database | `DatabaseConstraintsIT`, applicatiestart | Geslaagd lokaal: V1 t/m V9 op een lege database, Hibernate-validatie akkoord |
| Applicatie start, bootstrap en OpenAPI | `java -jar …` met profiel `local` | Geslaagd lokaal: health UP, platformbeheerder en Noeroel Islam aangemaakt, `/v3/api-docs` en Swagger UI bereikbaar |
| Integratietests met Testcontainers (PostgreSQL 17) | CI-job "Backend" | Geslaagd in CI (55 integratietests, PR #4); lokaal niet uitgevoerd omdat Docker ontbreekt |

### Beperkingen en open punten

- Accounts maakt een platformbeheerder aan; zelfregistratie en wachtwoordherstel volgen in fase 5 (D-018).
- Meldingsvoorkeuren worden opgeslagen, maar er worden nog geen pushmeldingen verstuurd (fase 5).
- Rate limiting en login-throttle zijn in-memory per instantie; bij meerdere instanties is een gedeelde opslag nodig.
- Verlopen refresh tokens worden nog niet automatisch opgeruimd (gepland voor fase 6, productievoorbereiding).
- Titel en duur van een YouTube-video worden niet automatisch opgehaald (D-021).
- Uploaden van afbeeldingen is er niet; activiteiten en media gebruiken een https-URL voor de afbeelding.

### Acceptatiecriteria fase 3

- [x] API's getest: 55 integratietests via HTTP tegen de echte applicatie, plus 49 unit tests.
- [x] Migraties reproduceerbaar: elke testrun bouwt het schema op een lege database op; Hibernate valideert.
- [x] Autorisatie server-side afgedwongen: `MosqueAccessService` bij elke beheeractie, lookups op (id, moskee),
  isolatietests A/B, en foreign keys op (id, mosque_id) in de database.
- [x] Video's niet in PostgreSQL; geen livestreaming.
- [x] CI-job "Backend" groen (PR #4, inclusief Testcontainers).

## Fase 4 — Beheerportaal

**Afhankelijkheden:** fase 1 en 3.

- Inloggen met de backend-authenticatie, route guards plus server-side autorisatie.
- Dashboard (alleen echte gegevens), evenementenbeheer, mediabeheer, aankondigingen.
- Duidelijk onderscheid tussen een aankondiging publiceren en een pushmelding versturen.
- Bevestiging bij risicovolle acties, formuliervalidatie, Nederlandse teksten.

**Acceptatiecriteria:** bevoegde beheerders beheren alle content zonder databasetoegang; unit tests voor
componenten, services, guards en formulieren; build groen.

## Fase 5 — Accounts, volgen en notificaties

**Afhankelijkheden:** fase 3 en 4; Firebase-project en Apple Developer-account voor echte pushmeldingen.

- Registratie, inloggen/uitloggen, JWT met roterende refresh tokens, wachtwoordherstel via e-mail.
- Volgen/ontvolgen en meldingsvoorkeuren per onderwerp: datamodel en API al in fase 3 (D-018); hier de koppeling
  aan registratie en verzending.
- FCM (Android, en iOS via APNs) met topics per moskee en onderwerp; server-side verzending.

**Acceptatiecriteria:** autorisatieregels getest; pushdoelgroepen afgeschermd per moskee en voorkeur; zonder
credentials alleen lokaal getest en zo gerapporteerd.

## Fase 6 — Testen, privacy, beveiliging en productievoorbereiding

**Afhankelijkheden:** fase 1 t/m 5; hosting en domein.

- Volledige testronde backend/Flutter/Angular, AVG-documentatie (bewaartermijnen, verwijderprocessen,
  toestemming), logging zonder persoonsgegevens, healthchecks, Docker-images, back-up en herstel,
  release- en rollbackprocedure.

**Acceptatiecriteria:** relevante tests slagen, buildstatus vastgelegd, resterende productieblokkades gedocumenteerd.

## Fase 7 — White-label voorbereiding

**Afhankelijkheden:** stabiele pilot.

- Brandconfiguratie per moskee (naam, ID's, kleuren, logo's, contact, push, backend) in een gevalideerd
  formaat; tokens en iconen gegenereerd uit die configuratie (de tokengenerator uit fase 1 is hier al op
  voorbereid). Flutter-flavors of build-time configuratie, CI-matrix per moskee, signing buiten de repo.
- Expliciete test: beheerder van moskee A kan niets van moskee B lezen of wijzigen.

**Acceptatiecriteria:** een tweede testmoskee vereist geen kopie van de codebase en geen ongecontroleerde toegang.

## Fase 8 — Latere uitbreidingen

Niet implementeren. Lidmaatschap, live-streaming en betalingen worden alleen gedocumenteerd als
uitbreidingspunten (zie D-011).
