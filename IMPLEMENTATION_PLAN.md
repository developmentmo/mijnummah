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
| 3 | Backend, database en API (vóór fase 2, zie D-004) | Voltooid en goedgekeurd (PR #4 gemerged) |
| 2 | UX/UI en Flutter-app | Voltooid en goedgekeurd (PR #5 gemerged) |
| 4 | Beheerportaal (Angular) | Voltooid en goedgekeurd (PR #6 gemerged) |
| 5 | Accounts, volgen en notificaties | Voltooid en goedgekeurd (PR #7 gemerged); echte e-mail en push niet getest (geen credentials) |
| 6 | Testen, privacy, beveiliging, productievoorbereiding | Voltooid en goedgekeurd (PR #9 gemerged); productieblokkades gedocumenteerd |
| 7 | Voorbereiding white-label automatisering | Getest en geslaagd (lokaal); Android- en iOS-builds per merk alleen in CI; wacht op beoordeling |
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

**Afhankelijkheden:** fase 1 en fase 3 (fase 3 is eerst uitgevoerd, D-004).

### Aanpak

- Vijf tabbladen (Home, Agenda, Lezingen, Ontdekken, Profiel) met `go_router` en `StatefulShellRoute`; elk tabblad
  houdt zijn eigen geschiedenis. Detailpagina's en inloggen openen over de tabbladen heen (D-026).
- Riverpod voor state, `http` voor de API-client met handgeschreven modellen op basis van de backend-DTO's (D-022).
- Tokens in de beveiligde opslag van het toestel; verversen loopt altijd één keer tegelijk (D-023).
- Offline: de laatst opgehaalde publieke gegevens worden bewaard en zonder verbinding getoond met een melding (D-024).
- Video's met `youtube_player_iframe` in de privacyvriendelijke modus, met een melding en "Bekijken op YouTube" als
  afspelen niet lukt (D-025).
- Datums in het Nederlands en in `Europe/Amsterdam`, ongeacht de tijdzone van het toestel.

### Opgeleverd

```text
frontend/app/lib/
  app.dart, main.dart          MaterialApp.router, ProviderScope, geen automatische herhaling bij fouten
  core/api/                    ApiClient (fouten, 401 + verversen, offline-cache), ApiException/NetworkException
  core/auth/                   AuthService (inloggen, uitloggen, verversen), SecureTokenStore
  core/config/                 AppConfig (--dart-define API_BASE_URL, MOSQUE_SLUG)
  core/format/dates.dart       Nederlandse notaties in Europe/Amsterdam
  core/routing/                router (5 tabbladen + details), AppShell (onderbalk of zijbalk)
  core/widgets/                laad/fout/leeg (AsyncView), offline-melding, labels, chips, infoRegels
  data/                        modellen, repositories (publiek en "mijn"), paginering
  features/home/               startpagina met losse secties per lijst
  features/agenda/             agenda met categoriefilter en "meer laden", detail, aanmelden
  features/lectures/           bibliotheek met zoeken en categorieën, detail met speler en gerelateerde content
  features/announcements/      aankondiging in detail
  features/discover/           profiel van de moskee, kennismaken, adres en contact, activiteitensoorten
  features/profile/            bezoeker of account, volgen met meldingsvoorkeuren, aanmeldingen, weergave
  features/auth/               inloggen
frontend/app/test/             unit-, widget- en schermtests met een nep-backend (alleen in tests)
android/app/src/main/AndroidManifest.xml  INTERNET-permissie, link-queries; http alleen in debug (src/debug)
ios/Runner/Info.plist          http alleen naar het lokale netwerk (ontwikkeling)
```

Schermen en wat ze doen:

| Scherm | Inhoud |
|---|---|
| Home | Logo, welkom (met naam als je bent ingelogd), uitgelichte aankondigingen, de eerstvolgende drie activiteiten met link naar de hele agenda, recente lezingen, nieuw toegevoegde video's, uitnodiging om te volgen. Een lege sectie verdwijnt; een mislukte sectie toont een herstelknop zonder de rest te blokkeren |
| Agenda | Aankomende activiteiten met datumblok, categorie, tijd en locatie; filter per categorie (ook via link, bijvoorbeeld vanuit Ontdekken); "meer laden"; geannuleerd zichtbaar |
| Activiteit | Afbeelding (als die er is), categorie, voor iedereen of alleen ingelogd, datum, tijd, locatie, beschrijving, aanmelden met vrije plaatsen, annuleren na bevestiging |
| Lezingen | Zoeken op titel en spreker (na een korte pauze in het typen), categorieën, kaarten met thumbnail, spreker, datum en duur |
| Lezing | Speler, titel, spreker, publicatie- en opnamedatum, duur, beschrijving, link naar de bijbehorende activiteit, "Meer zoals dit" |
| Ontdekken | Naam en beschrijving van de moskee, kennismaken, adres (opent de kaart), telefoon, e-mail, website, activiteitensoorten die de agenda gefilterd openen |
| Profiel | Bezoeker: uitleg en inloggen. Ingelogd: account, volgen en meldingsvoorkeuren, eigen komende aanmeldingen. Voor iedereen: uitleg bezoeker, account, volger en lid, en de keuze voor licht of donker |
| Inloggen | E-mail en wachtwoord met controle vooraf en begrijpelijke foutmeldingen (onjuist, geblokkeerd, te veel pogingen met wachttijd, geen verbinding) |

### Tests en controles

| Controle | Commando | Resultaat (11 okt 2026) |
|---|---|---|
| Formattering | `dart format --set-exit-if-changed …` | Geslaagd lokaal |
| Analyse | `flutter analyze` | Geslaagd lokaal, geen meldingen |
| Unit- en widgettests: datums (zomer- en wintertijd, middernacht), API-client (fouten, 401 en verversen, offline-cache), authenticatie (één verversing tegelijk, ingetrokken sessie, uitloggen offline), modellen, alle schermen en toestanden, aanmelden zonder dubbele aanmelding, inloggen, volgen en voorkeuren, navigatie, overflow op een kleine telefoon met 200% tekst en op een tablet | `flutter test` | Geslaagd lokaal, 116 tests |
| Geen losse kleuren of fonts | `node design/tokens/check-hardcoded.mjs` | Geslaagd lokaal |
| Doorlopen tegen de echte backend | Webversie van de app (alleen in een tijdelijke map, niet in de repository) met Playwright tegen de backend met profiel `local` en testcontent | Geslaagd lokaal: home, agenda en filter, activiteit, inloggen (fout en goed), aanmelden, volgen en voorkeuren, lezingen, lezing (melding bij onbereikbare video), ontdekken. Alleen de verwachte API-fouten (401 bij het foute wachtwoord, 404 `NOT_FOLLOWING` voor het volgen) |
| Android-build | `flutter build apk --debug` | Geslaagd in CI (GitHub Actions, PR #5); lokaal niet mogelijk omdat de Android SDK hier niet te downloaden is |

### Beperkingen en open punten

- Niet getest op een echt Android- of iOS-toestel of emulator; die zijn in deze omgeving niet beschikbaar. De
  YouTube-speler is daardoor alleen in de webversie gecontroleerd, en daar was YouTube onbereikbaar (de melding met
  "Bekijken op YouTube" verscheen na 15 seconden, zoals bedoeld). iOS-builds kunnen alleen op macOS.
- "Aanbevolen lezingen" op Home zijn de recentste items in de categorie Lezing; de backend heeft nog geen veld om
  content handmatig aan te bevelen (D-026).
- Zelf een account aanmaken en wachtwoordherstel volgen in fase 5; de app zegt dat eerlijk en verwijst naar de moskee.
- Meldingsvoorkeuren worden opgeslagen, maar pushmeldingen worden nog niet verstuurd (fase 5); de app zegt dat erbij.
- Officieel lidmaatschap bestaat niet in de app (fase 8); Profiel legt het verschil met volgen uit.
- Offline zijn alleen eerder geopende publieke schermen beschikbaar; aanmelden en volgen vereisen verbinding.
- De contactgegevens en het adres in de testcontent zijn voorbeelden; de echte gegevens vult de beheerder in (fase 4).

### Acceptatiecriteria fase 2

- [x] Alle geïmplementeerde schermen zijn doorlopen tegen de echte backend (webversie, zie hierboven).
- [x] Widgettests voor kernschermen en toestanden (laden, leeg, fout met opnieuw proberen, offline).
- [x] Geen dode knoppen of mockdata in productieflows: elke knop doet iets echts of de functie is weggelaten met uitleg.
- [x] Nederlandse datums en tijden in `Europe/Amsterdam`.
- [x] Toegankelijkheid: labels voor screenreaders, tekst tot 200% zonder overflow, focus- en drukstatussen uit het thema,
  contrast uit de tokens.
- [x] Android-build groen in CI (PR #5).

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

### Aanpak

- Angular 22 met standalone componenten, signals en `OnPush`; Angular Material voor formulieren, tabellen en dialogen.
- Het adres van de backend staat in `public/config.json` en wordt vóór het opstarten geladen, zodat één build in elke
  omgeving werkt (D-027).
- Toegangstoken alleen in het geheugen, verversingstoken in `sessionStorage`; verversen loopt één keer tegelijk en een
  401 leidt tot één nieuwe poging (D-028).
- De moskee staat in de URL (`/m/:mosqueId/...`). Guards spiegelen de rollen in de interface; de backend blijft de
  echte controle doen (D-031).
- Publiceren en een pushmelding versturen zijn gescheiden. Publiceren verstuurt nooit een pushmelding, en verzenden
  bestaat nog niet; het portaal zegt dat bij elke aankondiging (D-029).
- Tijden worden ingevoerd en getoond in de tijdzone van de moskee (standaard `Europe/Amsterdam`), ongeacht de tijdzone
  van de browser.
- Backend: de beheerlijst van activiteiten sorteert oplopend als er een `from` is meegegeven, zodat het dashboard de
  eerstvolgende activiteiten krijgt (D-030).

### Opgeleverd

```text
frontend/webportaal/
  public/config.json           apiBaseUrl (runtimeconfiguratie)
  src/main.ts                  config laden, daarna opstarten; Nederlandse melding als dat mislukt
  src/styles/_portal.scss      gedeelde stijlen voor lijsten, formulieren en zijpanelen
  src/app/core/api/            AdminApi (alle beheer-endpoints), modellen, ApiError en Nederlandse foutteksten
  src/app/core/auth/           AuthService, interceptor, guards (ingelogd, gast, platformbeheerder), veilige terug-URL
  src/app/core/mosque/         MosqueContext (moskee en rol), mosqueGuard en mosqueAdminGuard
  src/app/core/format/         Nederlandse labels, datums en duur in de tijdzone van de moskee
  src/app/shared/              bevestigingsdialoog, meldingen, paginakop, statuslabel, formulierhulpen, niet-opgeslagen-guard
  src/app/features/            shell, inloggen, moskee kiezen, overzicht, agenda, aanmeldingen, lezingen en video's,
                               aankondigingen, moskeegegevens, beheerders, accounts
  src/app/testing/harness.ts   testhulpen (alleen in tests)
backend/.../AdminEventController.java   sortering bij `from` (D-030), met integratietest
```

Schermen en wat ze doen:

| Scherm | Inhoud |
|---|---|
| Inloggen | E-mail en wachtwoord met controle vooraf; meldingen bij onjuiste gegevens, geblokkeerd account, te veel pogingen (met wachttijd), geen verbinding en een verlopen sessie. Na inloggen terug naar de pagina die eerst gevraagd werd |
| Moskee kiezen | Alleen bij meer dan één moskee; anders direct door. Lege toestand met uitloggen |
| Overzicht | Begroeting, snelle acties, de eerstvolgende vijf gepubliceerde activiteiten en vier tellers (conceptactiviteiten, gepubliceerde en concept-video's, gepubliceerde aankondigingen). Elk blok heeft eigen laad- en foutstatus |
| Agenda | Lijst met filter op status, paginering in de URL; nieuwe activiteit |
| Activiteit | Titel, beschrijving, categorie, zichtbaarheid, begin en einde, locatie, afbeelding-URL, aanmelden met capaciteit. Publiceren, depubliceren, annuleren en verwijderen met bevestiging; acties pas na opslaan; geannuleerd is alleen-lezen. Waarschuwing bij weggaan met niet-opgeslagen wijzigingen |
| Aanmeldingen | Alleen voor beheerders: wie zich heeft aangemeld, met een privacynotitie |
| Lezingen en video's | Lijst met statusfilter en zoeken; formulier met YouTube-link (met thumbnail), spreker, categorie, duur (u:mm:ss of minuten), opnamedatum en gekoppelde activiteit. Ongeldige of dubbele links komen bij het juiste veld |
| Aankondigingen | Lijst met "Verlopen"-label; formulier met verloopdatum (zonder tijd wordt dat 23:59). De knop heet "Publiceren in de app" en de bevestiging zegt dat er géén pushmelding gaat; een paneel "Pushmelding" legt uit dat verzenden nog niet beschikbaar is |
| Moskeegegevens | Profiel, adres en contact; wijzigingen ongedaan maken; waarschuwing bij weggaan |
| Beheerders | Toevoegen op e-mail met rol, rol wijzigen en verwijderen met bevestiging; wie zijn eigen toegang wijzigt, wordt doorgestuurd |
| Accounts | Alleen voor platformbeheerders: account aanmaken (wachtwoord minimaal 12 tekens, extra bevestiging voor platformbeheerder), blokkeren en deblokkeren met bevestiging, niet het eigen account |

### Tests en controles

| Controle | Commando | Resultaat (11 okt 2026) |
|---|---|---|
| Formattering | `npm run format:check` | Geslaagd lokaal |
| Lint | `npm run lint` | Geslaagd lokaal |
| Unit- en componenttests: datums en tijdzones, foutteksten, AuthService (herstellen, verversen één keer tegelijk, uitloggen offline), interceptor (401 en opnieuw proberen), alle guards, formulieren (validatie, omzetten naar verzoeken), inloggen, overzicht, agenda, activiteit (alle statusacties en bevestigingen), aankondiging (publiceren zonder push), video, moskee kiezen, shell, beheerders, accounts, bevestigingsdialoog, niet-opgeslagen-guard | `npm test` | Geslaagd lokaal, 24 bestanden, 102 tests |
| Build | `npm run build` | Geslaagd lokaal, eerste bundel 330,88 kB (budget 500 kB), geen waarschuwingen |
| Geen losse kleuren of fonts | `node design/tokens/check-hardcoded.mjs` | Geslaagd lokaal |
| Tokens actueel | `node design/tokens/build.mjs --check` | Geslaagd lokaal |
| Backend | `./mvnw verify` | Geslaagd lokaal tegen PostgreSQL (EventFlowIT 13 tests, inclusief de nieuwe sortering) |
| Doorlopen tegen de echte backend | Playwright-script (alleen in een tijdelijke map) tegen de backend met profiel `local` | Geslaagd lokaal: deep link naar inloggen en terug, fout en goed wachtwoord, moskee kiezen, overzicht, agenda-filter, validatie (einde vóór begin), activiteit aanmaken en publiceren (in een browser met tijdzone New York wordt 18:30 Amsterdamse tijd correct als 17:30 UTC (wintertijd) opgeslagen en verschijnt hij in de publieke API), niet-opgeslagen-dialoog, annuleren afgewezen, aanmeldingen, video met ongeldige en geldige link, aankondiging publiceren, moskeegegevens, onbekende beheerder, accounts, telefoon in donkere modus zonder horizontaal scrollen, uitloggen. Alleen de verwachte API-fouten (401 bij het foute wachtwoord, 404 bij de onbekende beheerder) |

### Beperkingen en open punten

- Afbeeldingen uploaden kan nog niet; een activiteit heeft een veld voor een afbeelding-URL. Opslag voor uploads hoort
  bij fase 6.
- Pushmeldingen versturen bestaat nog niet (fase 5); het portaal zegt dat expliciet. Inmiddels opgelost in fase 5.
- De weergave van datumvelden (dag-maand of maand-dag) hangt af van de taalinstelling van de browser.
- Niet getest op echte apparaten; wel in Chromium op desktop- en telefoonformaat.
- De teller "Gepubliceerde aankondigingen" telt ook verlopen aankondigingen mee.
- YouTube-thumbnails waren in deze omgeving niet bereikbaar; het formulier werkt zonder.

### Acceptatiecriteria fase 4

- [x] Bevoegde beheerders beheren activiteiten, video's, aankondigingen, moskeegegevens en beheerders zonder
  databasetoegang (doorlopen tegen de echte backend).
- [x] Route guards in het portaal, autorisatie blijft server-side (bestaande integratietests uit fase 3).
- [x] Publiceren en pushmelding zijn duidelijk gescheiden.
- [x] Bevestiging bij risicovolle acties, formuliervalidatie, Nederlandse teksten.
- [x] Unit tests voor componenten, services, guards en formulieren.
- [x] Build groen lokaal. CI: zie PR.

## Fase 5 — Accounts, volgen en notificaties

**Afhankelijkheden:** fase 3 en 4; Firebase-project en Apple Developer-account voor echte pushmeldingen.

**Status:** Getest en geslaagd lokaal. Echte e-mail (SMTP) en echte pushmeldingen (FCM en APNs) zijn niet getest:
daarvoor ontbreken credentials. Ze zijn getest tegen lokale testdubbels en met de `log`-providers.

### Aanpak

- Registratie in de app maakt een gewoon account (rol USER) en logt direct in. Bestaande regels blijven: wachtwoord
  minimaal 12 tekens, roterende refresh tokens, limieten per IP.
- Wachtwoordherstel met een code per e-mail (D-036); wachtwoord wijzigen en account verwijderen in de app (D-037).
- Pushdoelgroep wordt op de server bepaald uit volgen, voorkeuren, moskee en actieve accounts, niet met FCM-topics
  (D-032). Alleen beheerders versturen, één keer per item, alleen over gepubliceerde content (D-033).
- E-mail en push hebben een provider `none`, een echte (`smtp`, `fcm`) en `log` voor lokaal (D-035). FCM zonder
  Admin SDK (D-038).
- App: Firebase-instellingen via `--dart-define`; toestemming pas bij volgen of een onderwerp aanzetten (D-039).

### Opgeleverd

| Onderdeel | Wat |
|---|---|
| Backend `identity` | `POST /auth/register`, `POST /auth/password-reset`, `POST /auth/password-reset/confirm`, `PUT /me/password`, `POST /me/deletion`; herstelcodes (V10), limieten per IP en per adres |
| Backend `common/mail` | `Mailer` met SMTP, log en "niet ingesteld"; Nederlandse herstel-e-mail, asynchroon na de commit |
| Backend `notification` (nieuwe module) | Toestellen (`PUT /me/devices`, `POST /me/devices/remove`), verzenden per content (`POST /admin/mosques/{m}/{announcements,events,media}/{id}/push`), bereik vooraf (`GET .../notifications/audience`), status (`GET .../notifications`); FCM HTTP v1 met serviceaccount; ongeldige tokens worden opgeruimd (V11) |
| Backend overig | `FollowerDirectory` (volgers met onderwerp aan), `Event.isOver`, aanmeldingen verdwijnen met het account (V12), fout `503` voor diensten die niet zijn ingesteld |
| Portaal | Paneel "Pushmelding" bij aankondiging, activiteit en video: bereik, waarschuwing "Testomgeving" bij de log-provider, optionele titel en tekst, bevestiging, resultaat en status; alleen voor beheerders en alleen bij gepubliceerde content |
| App | Schermen Account aanmaken, Wachtwoord vergeten (code aanvragen en bevestigen) en Account (wachtwoord wijzigen, account verwijderen); links vanaf inloggen en profiel; pushmeldingen: toestemming, aan- en afmelden van het toestel, tikken opent het juiste scherm, melding onderin als de app open is; eerlijke melding als push niet is ingesteld of geweigerd |

### Tests en controles

| Controle | Commando | Resultaat (11 okt 2026) |
|---|---|---|
| Backend unit- en integratietests, nieuw: registratie, wachtwoord wijzigen (alle sessies beëindigd), account verwijderen (persoonlijke data weg, plek bij activiteit vrij, beheerder geweigerd), herstel zonder e-mail, herstel met code (eenmalig, verlopen, na vijf foute pogingen ongeldig, nieuwe code vervangt oude, per-adreslimiet zonder verschil in antwoord), toestel wisselt van gebruiker, doelgroep (alleen actieve volgers van deze moskee met het onderwerp aan), alleen ADMIN en één keer en alleen gepubliceerd, ongeldige tokens opgeruimd, standaardteksten, FCM-gateway tegen een stub die de JWT-handtekening controleert, limiet op wachtwoordcontrole | `./mvnw verify` | Geslaagd lokaal tegen PostgreSQL: 68 unit- en 73 integratietests |
| Portaal | `npm test`, `npm run lint`, `npm run format:check`, `npm run build` | Geslaagd lokaal: 25 bestanden, 115 tests; eerste bundel 330,98 kB |
| App | `dart format`, `flutter analyze`, `flutter test` | Geslaagd lokaal: 154 tests, geen analysemeldingen |
| App Android-build | `flutter build apk --debug` | Niet lokaal uitgevoerd (geen Android SDK in deze omgeving); CI bouwt de APK |
| API doorlopen tegen de echte backend (profiel `local`) | Python-script in een tijdelijke map | Geslaagd lokaal: registreren (te kort wachtwoord 400, dubbel adres 409), volgen, toestel aanmelden, herstelcode aanvragen (ook voor een onbekend adres 202), code uit het log, foute code 400, goede code inloggen, code tweede keer 400, wachtwoord wijzigen (fout huidig wachtwoord 400), account verwijderen (fout wachtwoord 400, daarna inloggen 401), beheerder verwijderen 409 |
| Portaal doorlopen (Playwright) | Script in een tijdelijke map | Geslaagd lokaal: concept geeft uitleg in plaats van verstuurknop, na publiceren bereik "2 volgers op 1 apparaat" en "Testomgeving", bevestigen, resultaat "Afgeleverd op 1 apparaat", na herladen geen verstuurknop meer. Geen API- of consolefouten |
| App doorlopen (webbuild, Playwright) | Script in een tijdelijke map | Geslaagd lokaal: bezoeker, account aanmaken (te kort wachtwoord geweigerd), ingelogd, volgen, melding "push niet ingesteld" (web), accountinstellingen, uitloggen, wachtwoord vergeten met code uit het log, ingelogd met het nieuwe wachtwoord |

### Beperkingen en open punten

- Echte SMTP en echte FCM/APNs zijn niet getest; daarvoor zijn een e-mailprovider, een Firebase-project en een Apple
  Developer-account nodig. Instellen: zie `backend/README.md` en `frontend/app/README.md`.
- iOS: de capability Push Notifications (entitlement `aps-environment`) moet in Xcode worden aangezet en een APNs-sleutel
  in Firebase worden gezet; niet gedaan en niet getest.
- Een melding die halverwege het verzenden stopt (herstart van de backend), blijft "wordt verstuurd" (D-034).
- Verloopt een sessie zonder uitloggen, dan blijft het toestel aangemeld tot de volgende keer inloggen of tot FCM het
  token ongeldig meldt (D-039).
- Herstel-e-mail is platte tekst, zonder logo of eigen opmaak per moskee.
- Niet getest op echte telefoons; de app is getest met widgettests en als webbuild in Chromium.

### Acceptatiecriteria fase 5

- [x] Registratie, inloggen en uitloggen, JWT met roterende refresh tokens, wachtwoordherstel via e-mail (e-mail lokaal
  via de log-provider).
- [x] Volgen en meldingsvoorkeuren gekoppeld aan registratie en verzending.
- [x] FCM-verzending server-side; app meldt toestellen aan en verwerkt meldingen (zonder echte credentials alleen lokaal
  getest).
- [x] Autorisatieregels getest (alleen ADMIN verstuurt, alleen eigen moskee, eigen account).
- [x] Pushdoelgroepen afgeschermd per moskee en voorkeur (integratietest).
- [x] Zonder credentials alleen lokaal getest en zo gerapporteerd.

## Fase 6 — Testen, privacy, beveiliging en productievoorbereiding

**Afhankelijkheden:** fase 1 t/m 5; hosting en domein.

**Status:** Getest en geslaagd lokaal. Wat alleen met hosting of credentials kan (echte server, SMTP, FCM/APNs,
stores), staat onder "Productieblokkades" in [DEPLOYMENT.md](DEPLOYMENT.md#productieblokkades).

### Aanpak

- Eerst de gaten zoeken: wat bewaren we te lang, wat lekt in logs, welke route is niet getest, wat ontbreekt om te
  kunnen deployen. Daarna gericht oplossen en testen, zonder nieuwe afhankelijkheden in de code.
- Privacy en productie als documenten die de moskee en een beheerder kunnen gebruiken: [PRIVACY.md](PRIVACY.md) en
  [DEPLOYMENT.md](DEPLOYMENT.md).

### Opgeleverd

| Onderdeel | Wat |
|---|---|
| Bewaartermijnen (D-040) | Nachtelijke opschoning van verlopen sessies (7 dagen), herstelcodes (1 dag), aanmeldingen (365 dagen na de activiteit) en pushtokens (270 dagen niet gezien); instelbaar met `RETENTION_*` |
| Hangende pushmeldingen (D-041) | Na 30 minuten op "wordt verstuurd" automatisch "mislukt"; lost D-034 op |
| Loghygiëne (D-043) | Geen e-mailadressen meer in start- en foutmeldingen; `log`-providers weigeren buiten `local`/`test` |
| API-beveiliging (D-042) | CSP en Referrer-Policy op alle API-antwoorden; test die elke route afloopt (anoniem 401, gebruiker zonder rol nooit toegang tot beheer); pushroutes in de tenant-isolatietest |
| Containers (D-044) | Dockerfiles voor backend en portaal (zonder rootrechten, healthchecks), portaal-nginx met CSP-nonce, `config.json` uit `API_BASE_URL`, SPA-routes |
| Productiestack | `deploy/docker-compose.prod.yml`, `.env.example`, `backup.sh` (dump, controle, rotatie), `restore.sh` (alleen in een nieuwe database) |
| CI | Nieuwe job "Containers en productiestack": bouwt beide images, start de stack, rooktest, back-up en herstel |
| Health | Mailhealthcheck uit, zodat een SMTP-storing de API niet `DOWN` meldt |
| Portaal | Uitleg over beeldrechten (afbeelding activiteit) en publicatierechten (video) |
| App | Pushtoken pas na toestemming (D-045); schermformaattests voor account aanmaken, wachtwoord vergeten, profiel en account (kleine telefoon met 200% tekst, tablet) |
| Documentatie | PRIVACY.md (gegevens, doelen, grondslagen, toegang, termijnen, verwijderen, toestemming push, logging, beeldrechten, externe diensten, rechten, open punten), DEPLOYMENT.md (configuratie, geheimen, eerste start, back-up en herstel, logging en monitoring, healthchecks, release en rollback, productieblokkades) |

### Tests en controles

| Controle | Commando | Resultaat (11 okt 2026) |
|---|---|---|
| Backend unit- en integratietests, nieuw: opschoning per termijn (net vóór en net na de grens), hangende melding één keer mislukt, `LocalOnly`, route-sweep anoniem en zonder rol, API-headers, pushroutes tussen moskeeën | `./mvnw verify` | Geslaagd lokaal tegen PostgreSQL: 70 unit- en 80 integratietests |
| Portaal | `npm test`, `npm run lint`, `npm run format:check`, `npm run build` | Geslaagd lokaal: 25 bestanden, 117 tests; eerste bundel 330,98 kB |
| App | `dart format`, `flutter analyze`, `flutter test` | Geslaagd lokaal: 162 tests, geen analysemeldingen |
| App Android-build | `flutter build apk --debug` | Niet lokaal (geen Android SDK); CI bouwt de APK |
| App iOS-build | | Niet uitgevoerd (vraagt macOS met Xcode) |
| Productiestack lokaal | `docker compose -f deploy/docker-compose.prod.yml up` met de runtime-images | Geslaagd lokaal: drie containers `healthy`, inloggen in het portaal via de containers zonder console- of CSP-fouten, health `UP`, JSON-logs zonder e-mailadressen |
| Back-up en herstel | `deploy/backup.sh`, `deploy/restore.sh` | Geslaagd lokaal: dump gecontroleerd, hersteld in een nieuwe database met dezelfde aantallen; live database en bestaande naam geweigerd |
| Volledige Docker-build | `docker build` | Lokaal niet mogelijk (build-container zonder internet in deze omgeving); CI-job bouwt en test |

### Beperkingen en open punten

- Geen echte server getest: hosting, TLS, DNS, externe back-upopslag en uptimebewaking ontbreken nog.
- Echte SMTP en FCM/APNs nog steeds niet getest (geen credentials). Ook "pushtoken pas na toestemming" is niet op een
  toestel gecontroleerd.
- Juridische punten voor de moskee: art. 9 AVG, privacyverklaring op een vaste URL, verwerkersovereenkomsten
  (PRIVACY.md, "Open punten").
- Inzage en export van gegevens zijn handmatig; inactieve accounts worden niet automatisch verwijderd.
- Opschoning draait in elke backend; bij meerdere instanties dubbel (onschuldig, D-040).
- Geen load- of penetratietest uitgevoerd.

### Acceptatiecriteria fase 6

- [x] Relevante tests slagen (backend, portaal, app) lokaal; CI: zie PR.
- [x] Buildstatus vastgelegd (tabel hierboven).
- [x] Privacy: dataminimalisatie, bewaartermijnen en verwijderprocessen, toestemming voor push, geen gevoelige data in
  logs, beeldrechten, geen tracking (PRIVACY.md).
- [x] Productievoorbereiding: configuratie, geheimen, back-up en herstel, logging en monitoring, healthchecks,
  containers, deployment, release en rollback (DEPLOYMENT.md).
- [x] Resterende productieblokkades gedocumenteerd (DEPLOYMENT.md).

## Fase 7 — White-label voorbereiding

**Afhankelijkheden:** stabiele pilot (fase 1 t/m 6).

**Status:** Getest en geslaagd lokaal. De Android- en iOS-builds per merk kunnen hier niet (geen Android SDK, geen
macOS) en draaien in CI. Werkwijze en runbook: [WHITE_LABEL.md](WHITE_LABEL.md).

### Aanpak

- Eerst inventariseren wat moskeespecifiek was: de naam stond in app, portaal, Android-manifest, `Info.plist` en
  tests; ID's in Gradle en Xcode; kleuren als "green" en "gold" in de tokens; het beeldmerk als code in de
  logogenerator. Daarna één configuratie per moskee, waar alles uit wordt gegenereerd (D-046, D-047).
- Een fictieve tweede moskee (`brands/voorbeeldmoskee/`) om te bewijzen dat het zonder kopie van de code werkt, en
  om dat in CI te blijven bewijzen.
- Zoeken naar plekken waar gegevens van moskeeën kunnen mengen nu er meerdere apps zijn. Gevonden: pushtokens hoorden
  niet bij een app (opgelost, D-049). Verder de bestaande tenant-isolatietest uitgebreid.

### Opgeleverd

| Onderdeel | Wat |
|---|---|
| Merkconfiguratie (D-046) | `brands/<slug>/brand.json` met schema (`brands/brand.schema.json`) en beeldmerk `mark.svg`; controle op schema, beeldmerk (alleen merkkleuren, geen scripts, tekst of links), dubbele ID's en contrast |
| Tooling (D-047) | `tools/brand/brand.mjs`: `list`, `validate`, `new` (kleurschalen uit twee merkkleuren, tijdelijk beeldmerk), `apply`, `check`, `show` |
| Tokens en assets (D-048) | Paletten `primary`/`accent` (eventueel `neutral`) uit het merk; donkere modus draait mee met de merkkleur; logo's, app-iconen en splash uit `mark.svg`; pilotbestanden byte-voor-byte gelijk gebleven |
| Android | Application ID en naam onder het icoon uit `brand.properties`; release-ondertekening uit omgevingsvariabelen |
| iOS | Bundle ID en naam onder het icoon uit `Brand.xcconfig` |
| App | Naam, slug en privacylink uit `brand.g.dart`; `MosqueApp` in plaats van `NoeroelIslamApp`; privacyverklaring in Profiel als het merk een link heeft; pushtoken aangemeld voor de moskee van de app |
| Portaal | Naam uit het merk (titel, logo, navigatie); *Moskee toevoegen* voor platformbeheerders (slug volgt de naam, controle op formaat en dubbele slug) |
| Backend (D-049) | V13: `push_device.mosque_id`; `PUT /me/devices` vraagt `mosqueSlug`; versturen en bereik tellen alleen toestellen in de app van de moskee |
| Testmoskee | `brands/voorbeeldmoskee/`: ander palet, beeldmerk en ID's; fictief, niet publiceren |
| CI | Merken valideren en `check`; per merk een job met analyse, tests en een getekende release-AAB (wegwerpsleutel, controle op handtekening en application ID); per merk een iOS-simulatorbuild met controle op bundle ID en naam |
| Release (D-051) | `.github/workflows/release-app.yml`: handmatig per merk en platform, sleutels en Firebase-waarden uit een GitHub Environment, artifact zonder store-upload; iOS ongetekend tot er een Apple-account is |
| Documentatie | WHITE_LABEL.md (wat per moskee, wat centraal, runbook, ondertekening, release, isolatie, uitbreiden), D-046 t/m D-051, API.md, DATABASE.md, READMEs, BRAND_GUIDELINES.md |

### Tests en controles

| Controle | Commando | Resultaat (16 okt 2026) |
|---|---|---|
| Backend, nieuw: melding alleen naar de app van de verzendende moskee, token verhuist met de app, `mosqueSlug` verplicht en onbekende moskee `404`; nieuwe moskee geeft niemand toegang, (de)activeren en moskee aanmaken alleen door platformbeheerder, rol van B niet via A te wijzigen | `./mvnw verify` | Geslaagd lokaal tegen PostgreSQL: 70 unit- en 82 integratietests |
| Migratie V13, overzetten van bestaande tokens | Los uitgevoerd in een tijdelijke database | Eén moskee: tokens krijgen die moskee; twee moskeeën: blijven leeg |
| Merken | `brand.mjs validate`, `brand.mjs check` | Geslaagd: beide merken geldig, 107 van 107 kleurparen per merk; repository geeft `noeroel-islam` weer |
| App met elk merk | `dart format`, `flutter analyze`, `flutter test` | Geslaagd lokaal met `noeroel-islam` en met `voorbeeldmoskee`: 164 tests, geen analysemeldingen |
| Portaal | `npm test`, `npm run lint`, `npm run format:check`, `npm run build` | Geslaagd lokaal: 26 bestanden, 121 tests; eerste bundel 331,27 kB; ook gebouwd met het testmerk (licht en donker bekeken) |
| Heen en terug wisselen van merk | `apply voorbeeldmoskee`, daarna `apply` | Alle bestanden weer exact gelijk aan de repository |
| Android-release per merk (AAB, getekend) | CI-job "Flutter-app (merk)" | Niet lokaal (geen Android SDK); CI |
| iOS-build per merk | CI-job "iOS-build (merk)" | Niet lokaal (geen macOS); CI |
| Release-workflow | `release-app.yml` | Niet uitgevoerd: er zijn nog geen Environments, sleutels of Firebase-apps; `noeroel-islam` heeft nog geen `backend.apiBaseUrl` |

### Beperkingen en open punten

- Geen echte release gebouwd of getekend: Play Console-account, uploadsleutel, Apple Developer-account, Firebase-apps
  en productie-API-adres ontbreken nog. De release-workflow faalt bewust zolang die er niet zijn.
- iOS wordt ongetekend gebouwd; ondertekenen en uploaden vragen een Apple Developer-account.
- Het portaal draagt het merk van Noeroel Islam, ook voor beheerders van andere moskeeën (D-050).
- Typografie en categorieën zijn voor alle moskeeën gelijk; uitbreiden staat in WHITE_LABEL.md.
- Het beeldmerk van de testmoskee is een eenvoudige tekening, bedoeld voor tests.

### Acceptatiecriteria fase 7

- [x] Merk- en moskeespecifieke instellingen gescheiden van de functionaliteit, in een gevalideerd formaat met
  duidelijke standaardwaarden (naam, ID's, logo's en iconen, kleuren, links, backend; typografie, contact,
  categorieën en push bewust centraal, gedocumenteerd).
- [x] Reproduceerbaar proces: merk aanmaken, controleren, assets genereren, bouwen per platform, testen, release
  voorbereiden; sleutels en certificaten buiten de repository; geen automatische store-publicatie.
- [x] Expliciet getest dat een beheerder van moskee A niets van moskee B kan lezen of wijzigen, ook niet via een
  nieuwe moskee of via pushmeldingen; vastgelegd wat per moskee wordt gegenereerd en wat centraal is.
- [x] Tweede testmoskee zonder kopie van de codebase (`brands/voorbeeldmoskee/`), lokaal getest; builds per merk in CI.

## Fase 8 — Latere uitbreidingen

Niet implementeren. Lidmaatschap, live-streaming en betalingen worden alleen gedocumenteerd als
uitbreidingspunten (zie D-011).
