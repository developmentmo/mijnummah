# Beslissingen

Belangrijke architectuur- en ontwerpbeslissingen. Status: **Voorgesteld** (wacht op akkoord),
**Besloten**, of **Vervangen** (met verwijzing naar de opvolger).

---

## D-001 — Nieuwbouw in een lege repository

- **Status:** Besloten (feitelijk)
- **Context:** `developmentmo/mijnummah` bevatte bij de analyse in fase 0 geen commits. Er is geen bestaande
  code om te behouden of gericht te verbeteren.
- **Besluit:** Alle onderdelen worden nieuw opgezet, in de fasevolgorde uit het implementatieplan.
  Scaffolding gebeurt zo laat mogelijk en alleen voor wat de fase nodig heeft.

## D-002 — Conventies overnemen uit eigen projecten

- **Status:** Voorgesteld
- **Context:** Het masterprompt vraagt bestaande conventies te volgen; die zijn er in deze repo niet.
  `developmentmo/repairtrack` (zelfde eigenaar) gebruikt een bewezen stack en structuur.
- **Besluit:** Dezelfde keuzes als RepairTrack waar ze passen: Spring Boot 4 / Java 21 / Maven, modulaire
  monoliet met Spring Modulith, lagen `api` → `application` → `domain` + `infrastructure`, records als DTO's,
  Flyway, PostgreSQL; Flutter met Riverpod, Dio en go_router.
- **Gevolg:** Herkenbaar voor de eigenaar, minder nieuwe keuzes. Geen microservices.

## D-003 — Monorepo met generieke platformnaam

- **Status:** Voorgesteld
- **Besluit:** Eén repository met `backend/`, `frontend/app/` (Flutter), `frontend/webportaal/` (Angular), `design/`,
  `deploy/` en `docs/`. De indeling van `backend/` en `frontend/` volgt de lokale RepairTrack-structuur van de
  eigenaar.
  Code en packages heten generiek (`mijnummah`, Java-package `nl.mijnummah`); "Noeroel Islam" komt alleen
  voor in brandconfiguratie, assets en content.
- **Gevolg:** Fase 7 (andere moskeeën) vraagt geen hernoemingen. Contracten tussen app, portaal en backend
  wijzigen in één PR.

## D-004 — Backend (fase 3) vóór de Flutter-schermen (fase 2)

- **Status:** Besloten (11 oktober 2026, door de eigenaar gekozen)
- **Context:** In het masterprompt komt de Flutter-app vóór de backend. Met een lege repo betekent dat schermen
  zonder echte API, terwijl mockdata in productieflows niet is toegestaan.
- **Besluit:** Volgorde 0 → 1 → 3 → 2 → 4 → 5 → 6 → 7. Fase 1 blijft ongewijzigd.
- **Afgewezen alternatief:** Originele volgorde aanhouden. Fase 2 bouwt dan tegen een vastgelegd OpenAPI-contract met
  een lokale testserver die alleen in tests en ontwikkeling draait, en fase 3 implementeert dat contract.

## D-005 — Eén bron voor design tokens, gegenereerd naar Flutter en Angular

- **Status:** Besloten (fase 1)
- **Context:** Het ontwerp moet in Flutter en Angular identiek en centraal zijn, en later per moskee
  verwisselbaar.
- **Besluit:** `design/tokens/tokens.json` in het W3C Design Tokens-formaat is de enige bron. Een eigen
  Node-script zonder dependencies genereert Dart-constanten en CSS custom properties. Het script heeft een
  `--check` modus voor CI. Thema's (Flutter `ThemeData` + `ThemeExtension`, Angular Material M3) gebruiken
  alleen de gegenereerde waarden.
- **Afgewogen:** Style Dictionary (extra dependency en configuratie voor een kleine set tokens);
  handmatig synchroniseren (loopt gegarandeerd uit de pas).

## D-006 — Tenantisolatie vanaf de eerste migratie

- **Status:** Besloten (uitgevoerd in fase 3, zie D-017)
- **Besluit:** Iedere moskeespecifieke tabel heeft een verplichte `mosque_id`. Beheerrechten zijn gekoppeld aan
  een moskee (`mosque_administrator`). Een `MosqueAccessService` controleert server-side bij elke
  beheeroperatie; een `mosqueId` uit de client wordt nooit zonder die controle vertrouwd. Publieke queries
  filteren altijd op moskee én publicatiestatus. Eén gedeelde database en backend; geen schema per tenant.
- **Gevolg:** Isolatietests (moskee A tegen B) horen al bij fase 3, niet pas bij fase 7.

## D-007 — Typografie: Fraunces + Inter, lokaal gebundeld

- **Status:** Besloten (fase 1)
- **Besluit:** Fraunces voor koppen (warm, eigentijds, spaarzaam gebruikt), Inter voor tekst en UI (zeer
  leesbaar, ook op kleine maten en voor ouderen), Noto Naskh Arabic voor Arabische tekst. Alle drie SIL Open
  Font License 1.1. De fontbestanden worden in de app en het portaal meegeleverd, met de licentie ernaast.
- **Reden:** Geen runtime-verzoeken naar Google Fonts (privacy/AVG, offline gebruik) en voorspelbare rendering.

## D-008 — Gebruik van goud en contrastregels

- **Status:** Besloten (fase 1)
- **Context:** Gemeten contrast: goud op ivoor 2,14:1, goud op wit 2,37:1, goud op donkergroen 5,16:1,
  jade op ivoor 4,69:1, jade op wit 5,19:1, donkergroen op ivoor 11,03:1.
- **Besluit:** Goud nooit als tekst- of iconkleur op lichte achtergronden; wel als decoratief accent of op
  donkergroen. Een donkere goudtint wordt toegevoegd voor tekst op licht. Alle tekstparen in de tokens
  worden in CI op WCAG AA gecontroleerd.

## D-009 — Video's via een providerinterface, nooit in PostgreSQL

- **Status:** Besloten (uitgevoerd in fase 3, zie D-021)
- **Besluit:** Een media-item bewaart metadata plus een providerreferentie (`provider`, `external_id`).
  Een `VideoProvider`-interface met als eerste adapter YouTube. Video-opslag en streaming blijven buiten de
  backend. Live-streaming wordt niet gebouwd; het model laat later een `LIVE`-type toe.
- **Open:** definitieve keuze van het videoplatform (zie het implementatieplan).

## D-010 — Authenticatie in de eigen backend

- **Status:** Besloten (uitgevoerd in fase 3 voor door de platformbeheerder aangemaakte accounts; zelf registreren in fase 5)
- **Besluit:** Spring Security met korte JWT access tokens en roterende, gehashte refresh tokens; wachtwoorden
  met een standaard adaptieve hash (BCrypt/Argon2 via Spring Security). Beheerders loggen in fase 3/4 op
  dezelfde manier in; publieke accounts volgen in fase 5. Geen zelfgebouwde cryptografie.
- **Afgewogen:** Keycloak (extra infrastructuur voor een pilot), Firebase Auth (extra afhankelijkheid van
  Google en minder controle over gebruikersdata).

## D-011 — Uitbreidingspunten voor fase 8 alleen documenteren

- **Status:** Voorgesteld
- **Besluit:** Volgen (`mosque_follow`) en lidmaatschap blijven gescheiden concepten zodat lidmaatschap later
  een eigen module wordt. Media-items krijgen een type zodat livestreams later passen. Er worden nu geen
  lege modules of abstracties voor lidmaatschap, live-streaming of betalingen gebouwd.

## D-012 — Fonts: statische instanties voor Flutter, variabele WOFF2 voor het web

- **Status:** Besloten (fase 1)
- **Context:** Flutter koppelt `FontWeight` niet automatisch aan de gewichtsas van een variabel font.
- **Besluit:** `design/fonts/build_fonts.py` maakt uit de variabele bronfonts statische TTF's per gewicht voor
  Flutter, en WOFF2-bestanden van de variabele fonts voor het portaal. Fraunces staat vast op SOFT 50 en WONK 0
  (rustige vorm). De bronfonts komen uit de officiële Google Fonts-repository.

## D-013 — App-ID's en projectnamen

- **Status:** Besloten (fase 1), definitief te bevestigen vóór de eerste store-release
- **Besluit:** Android `applicationId` en iOS bundle identifier `nl.mijnummah.noeroelislam`, weergavenaam
  "Noeroel Islam". De Dart-package heet generiek `mijnummah_app` (D-003). De Android-`namespace` blijft
  `nl.mijnummah.mijnummah_app`; die is intern en niet zichtbaar voor gebruikers.

## D-014 — Angular Material als componentbibliotheek van het portaal

- **Status:** Besloten (fase 1)
- **Besluit:** Angular Material (M3) voor formulieren, knoppen, dialogen en tabellen, met alle systeemkleuren,
  typografie en vormen gekoppeld aan de `--mu-*` tokens. Eigen componenten alleen waar Material niets passends
  heeft (melding, skeleton, lege toestand). Unit tests met Vitest (standaard in Angular 22), linting met
  angular-eslint.
- **Reden:** Toegankelijke, beproefde componenten voor bestuurders zonder technische kennis, zonder een eigen
  componentbibliotheek te hoeven onderhouden.

## D-015 — Geen golden tests

- **Status:** Besloten (fase 1)
- **Besluit:** Geen pixelvergelijkingen in CI. Widgettests controleren structuur, thema, overflow bij
  verschillende schermformaten en tekstschalen, en toegankelijkheid. Visuele controle gebeurt via de
  designpreviews.
- **Reden:** Fontrendering verschilt tussen macOS en Linux, waardoor goldens vals alarm geven.

## D-016 — Eigen uniforme foutrespons in plaats van RFC 9457

- **Status:** Besloten (fase 3)
- **Context:** Het plan noemde RFC 9457 Problem Details. RepairTrack (D-002) gebruikt een eigen, eenvoudiger
  formaat, en de app en het portaal hebben vooral een stabiele foutcode nodig.
- **Besluit:** Elke fout heeft de vorm `{timestamp, status, code, message, path, fieldErrors?}`. Clients reageren
  op `code`; `fieldErrors` geeft validatiefouten per veld voor formulieren. Spring's Problem Details staat uit.
- **Gevolg:** Eén formaat voor alle endpoints, ook voor 401/403 uit de securityfilters. Overstappen op RFC 9457
  kan later met behoud van `code` als extensieveld.

## D-017 — Rechten per moskee in de moskeemodule

- **Status:** Besloten (fase 3)
- **Besluit:** Twee niveaus. Platformrol op het account (`PLATFORM_ADMIN` of `USER`) en een rol per moskee in
  `mosque_administrator` (`ADMIN`: alles voor die moskee; `EDITOR`: alleen content, geen persoonsgegevens van
  aanmeldingen en geen beheerders). De moskeemodule bezit deze rollen en biedt `MosqueAccessService` aan; elke
  beheeractie roept die aan met de moskee uit de route en zoekt daarna objecten op met (id, moskee).
- **Gevolg:** Een route van een andere moskee geeft `403 MOSQUE_ACCESS_DENIED`; een ID van een andere moskee in
  de eigen route geeft `404`. De database dwingt bovendien af dat aanmeldingen en gerelateerde media niet naar een
  activiteit van een andere moskee kunnen wijzen. Een moskee houdt altijd minstens één ADMIN.

## D-018 — Volgen en meldingsvoorkeuren al in fase 3, pushmeldingen in fase 5

- **Status:** Besloten (fase 3)
- **Context:** Het masterprompt vraagt in fase 3 het datamodel en de API voor volgen en meldingsvoorkeuren; het
  plan had beide in fase 5 gezet, samen met registratie en push.
- **Besluit:** Tabellen en endpoints voor volgen/ontvolgen en voorkeuren per onderwerp zijn er nu, voor elk
  ingelogd account. Accounts worden tot fase 5 door een platformbeheerder aangemaakt (geen zelfregistratie).
  Pushmeldingen versturen blijft fase 5.
- **Gevolg:** De app (fase 2) kan volgen en voorkeuren tegen de echte API bouwen; fase 5 voegt registratie,
  wachtwoordherstel en verzending toe zonder schemawijziging van deze tabellen.

## D-019 — Levenscyclus en zichtbaarheid van content

- **Status:** Besloten (fase 3)
- **Besluit:** Nieuwe content is altijd een concept; publiceren is een aparte actie. Zichtbaarheid `PUBLIC`
  (iedereen) of `REGISTERED_USERS` (alleen ingelogd) op activiteiten en media is de toegangsinstelling.
  Een geannuleerde activiteit blijft zichtbaar met haar status en is definitief; een activiteit met aanmeldingen kan
  niet terug naar concept of verwijderd worden. Verlopen aankondigingen verdwijnen publiek. Een inactieve moskee
  verbergt al haar content.
- **Gevolg:** Wie zich heeft aangemeld, ziet altijd wat er met de activiteit gebeurt. Lidmaatschap als extra
  zichtbaarheidsniveau blijft een uitbreidingspunt (D-011).

## D-020 — Integratietests tegen echte PostgreSQL, met of zonder Docker

- **Status:** Besloten (fase 3)
- **Besluit:** Integratietests (`*IT`) draaien de volledige applicatie tegen PostgreSQL, nooit H2. Standaard via een
  PostgreSQL 17-Testcontainer (CI en machines met Docker). Is `TEST_DATABASE_URL` gezet, dan wordt die bestaande
  database gebruikt en per test geleegd.
- **Reden:** De ontwikkelomgeving van deze sessie heeft geen Docker; zo zijn dezelfde tests lokaal (PostgreSQL 16)
  en in CI (PostgreSQL 17) uit te voeren.

## D-021 — YouTube zonder API-sleutel, embed in privacyvriendelijke modus

- **Status:** Besloten (fase 3)
- **Besluit:** De YouTube-adapter herkent links en video-ID's en bouwt de kijk-, embed- en thumbnail-URL's zelf.
  Er worden geen YouTube-API's aangeroepen (geen sleutel, geen netwerkverkeer vanaf de backend). De embed-URL
  gebruikt `youtube-nocookie.com`. Titel, spreker en duur vult de beheerder in.
- **Gevolg:** Geen externe afhankelijkheid of quotum. Automatisch metadata ophalen kan later via een optionele
  adapter met eigen sleutel.

## D-022 — Riverpod, go_router en `http` in de app

- **Status:** Besloten (fase 2)
- **Besluit:** Riverpod 3 voor state en afhankelijkheden, go_router voor navigatie met een eigen geschiedenis per
  tabblad, en het `http`-pakket voor de API-client. Modellen zijn handgeschreven op basis van de backend-DTO's; geen
  codegeneratie.
- **Waarom:** Dio was in het plan genoemd, maar `http` met een eigen client van ongeveer 150 regels dekt wat nodig is
  (fouten, 401 en verversen, offline-cache) met een kleinere afhankelijkheid. De API is klein genoeg om de modellen
  met de hand bij te houden; codegeneratie uit OpenAPI kan later als de API groeit.
- **Gevolg:** Riverpod herhaalt mislukte verzoeken standaard automatisch; dat staat uit, zodat de gebruiker direct een
  melding met "Opnieuw proberen" ziet.

## D-023 — Tokens in de beveiligde opslag, één verversing tegelijk

- **Status:** Besloten (fase 2)
- **Besluit:** Access en refresh token staan in de Keychain (iOS) of versleuteld met de Android Keystore
  (`flutter_secure_storage`), nooit in gewone voorkeuren. Verloopt het access token bijna, of geeft de server `401`,
  dan ververst de app de sessie; gelijktijdige verzoeken wachten op dezelfde verversing.
- **Waarom:** Refresh tokens roteren en hergebruik trekt de hele sessie in (D-010). Twee gelijktijdige verversingen
  zouden de gebruiker daardoor uitloggen.
- **Gevolg:** Mislukt verversen met `401`, dan wordt de gebruiker bezoeker en blijft publieke content zichtbaar.

## D-024 — Offline: laatst opgehaalde publieke gegevens

- **Status:** Besloten (fase 2)
- **Besluit:** Elk publiek antwoord wordt bewaard (`shared_preferences`, per URL). Zonder verbinding toont de app die
  versie met de melding "Je bent offline" en het tijdstip van ophalen. Antwoorden voor de ingelogde gebruiker worden
  niet bewaard, en bij uitloggen wordt de cache gewist.
- **Waarom:** Het masterprompt vraagt om offline gebruik waar passend; een eenvoudige cache volstaat voor de pilot en
  er komen geen persoonsgegevens in.
- **Gevolg:** Alleen eerder geopende schermen werken offline. Aanmelden en volgen vereisen verbinding.

## D-025 — Video's afspelen met youtube_player_iframe

- **Status:** Besloten (fase 2)
- **Besluit:** De app speelt video's af met `youtube_player_iframe` (WebView met de officiële iFrame-speler) in de
  privacyvriendelijke modus. Bij een fout van de speler, of als hij na 15 seconden nog niet geladen is, verschijnt
  "Video niet beschikbaar" met een knop om de video bij YouTube te openen.
- **Waarom:** Geen API-sleutel nodig, de bediening van YouTube zelf (afspelen, volume, volledig scherm) en het past bij
  D-021. De speler zit achter een eigen widget, zodat een andere provider later kan worden toegevoegd.
- **Gevolg:** Video's die niet ingesloten mogen worden, spelen alleen bij YouTube af.

## D-026 — Afwijkingen en invullingen in de app

- **Status:** Besloten (fase 2)
- **Besluit:**
  - Detailpagina's en inloggen openen over de tabbladen heen (zonder onderbalk), zodat terug altijd naar het juiste
    tabblad gaat. Een direct geopende detailpagina heeft een knop naar Home.
  - "Aanbevolen lezingen" op Home zijn de recentste items in de categorie Lezing; "Nieuw toegevoegd" toont de
    recentste overige video's. Er is nog geen veld om content handmatig aan te bevelen.
  - `Europe/Amsterdam` is sinds tzdata 2022b een verwijzing naar `Europe/Brussels`. De compacte tijdzonedatabase in de
    app bevat geen verwijzingen; de app valt daarom terug op `Europe/Brussels` (sinds 1970 dezelfde regels).
  - De designpreview uit fase 1 blijft bereikbaar in debug-builds via Profiel > Ontwikkeling (`/design`).
- **Gevolg:** Een aanbevelingsveld kan later in de backend en het portaal worden toegevoegd zonder de app-structuur te
  wijzigen.

## D-027 — Runtimeconfiguratie van het portaal

- **Status:** Besloten (fase 4)
- **Besluit:** Het portaal leest het adres van de backend uit `public/config.json` (`apiBaseUrl`) voordat Angular
  opstart. Lukt dat niet, dan toont de pagina een Nederlandse foutmelding.
- **Waarom:** Eén build kan zo naar test, acceptatie en productie, en per moskee (white-label) zonder opnieuw te
  bouwen. Er staan geen geheimen in.
- **Gevolg:** Bij uitrollen wordt alleen `config.json` vervangen.

## D-028 — Tokens in het portaal

- **Status:** Besloten (fase 4)
- **Besluit:** Het toegangstoken staat alleen in het geheugen; het verversingstoken in `sessionStorage`. Verversen
  gebeurt 30 seconden vóór het verlopen en altijd één keer tegelijk. Een 401 leidt tot één verversing en één nieuwe
  poging; daarna gaat de gebruiker naar inloggen met de oorspronkelijke pagina als terug-URL (alleen interne adressen).
- **Waarom:** `sessionStorage` overleeft een herlaadactie maar niet het sluiten van het tabblad, wat past bij een
  beheeromgeving. Een httpOnly-cookie vraagt een andere backend-flow (CSRF), die nu niet nodig is.
- **Gevolg:** Een XSS-lek kan het verversingstoken lezen; daarom geen `innerHTML` met gebruikersinhoud en een strikte
  CSP in fase 6.

## D-029 — Publiceren is geen pushmelding

- **Status:** Besloten (fase 4)
- **Besluit:** Publiceren zet content zichtbaar in de app en verstuurt nooit een pushmelding. De knop heet "Publiceren
  in de app", de bevestiging zegt dat er géén pushmelding gaat, en een apart paneel "Pushmelding" meldt dat verzenden
  nog niet beschikbaar is. Er is geen knop die niets doet.
- **Waarom:** Het masterprompt vraagt een duidelijk onderscheid; verzenden vereist Firebase en APNs (fase 5).
- **Gevolg:** In fase 5 wordt het paneel een aparte, bevestigde actie.

## D-030 — Sortering van de beheerlijst van activiteiten

- **Status:** Besloten (fase 4)
- **Besluit:** `GET /api/v1/admin/mosques/{id}/events` sorteert aflopend op begintijd, behalve als `from` is meegegeven:
  dan oplopend (eerstvolgende eerst). Bij gelijke tijd beslist het id.
- **Waarom:** Het dashboard toont de eerstvolgende activiteiten; aflopend zou de verste toekomst eerst geven.
- **Gevolg:** Integratietest `adminListFromAMomentIsSoonestFirst`.

## D-031 — Moskee in de URL; rollen gespiegeld in de interface

- **Status:** Besloten (fase 4)
- **Besluit:** Alle moskeeschermen staan onder `/m/:mosqueId`. `mosqueGuard` kiest de moskee en weigert er een zonder
  toegang; `mosqueAdminGuard` beschermt Moskeegegevens, Beheerders en Aanmeldingen. Een platformbeheerder telt als
  beheerder. De interface verbergt wat een rol niet mag, maar de backend blijft elke actie controleren.
- **Waarom:** Links zijn deelbaar en herlaadbaar, en een gebruiker met meerdere moskeeën ziet altijd in welke moskee die werkt.
- **Gevolg:** Een gemanipuleerde URL levert hooguit een melding op; data van een andere moskee komt nooit binnen.
