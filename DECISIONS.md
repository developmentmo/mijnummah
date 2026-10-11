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

