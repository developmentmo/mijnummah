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

- **Status:** Besloten (fase 8)
- **Besluit:** Volgen (`mosque_follow`) en lidmaatschap blijven gescheiden concepten zodat lidmaatschap later
  een eigen module wordt. Er worden nu geen lege modules, tabellen of abstracties voor lidmaatschap, live-streaming
  of betalingen gebouwd. Hoe ze later passen staat in [EXTENSIONS.md](EXTENSIONS.md).
- **Bijgesteld:** het eerdere voornemen om media-items alvast een type (opname of live) te geven is niet uitgevoerd:
  zonder livestreams diende het geen doel. Livestreams passen via de bestaande `VideoProvider`; het type komt met
  de migratie die live-streaming toevoegt.

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

## D-032 — Pushdoelgroep op de server bepaald, niet met FCM-topics

- **Status:** Besloten (fase 5); vervangt het voorstel "topics per moskee en onderwerp" uit het plan
- **Besluit:** De app meldt zijn pushtoken aan bij de backend (`PUT /api/v1/me/devices`). Bij verzenden kiest de
  backend de ontvangers: volgers van díe moskee, met het onderwerp niet uitgezet, met een actief account, en van hen de
  aangemelde toestellen. Elk toestel krijgt een eigen bericht via FCM HTTP v1. Tokens die FCM als ongeldig meldt,
  worden verwijderd.
- **Waarom:** Met topics bepaalt het toestel zelf waarop het geabonneerd is; een geblokkeerd of verwijderd account, of
  een oude abonnering na ontvolgen, zou dan nog meldingen krijgen. Op de server is de doelgroep afdwingbaar en testbaar
  (afgeschermd per moskee en voorkeur, zoals de acceptatiecriteria vragen).
- **Gevolg:** Eén verzoek per toestel. Voor de omvang van één moskee is dat ruim voldoende; bij veel grotere aantallen
  kan later batching of een wachtrij nodig zijn.

## D-033 — Alleen beheerders versturen een pushmelding, één keer per item

- **Status:** Besloten (fase 5)
- **Besluit:** Een pushmelding versturen vraagt de rol ADMIN van de moskee (`SEND_NOTIFICATIONS`); een EDITOR kan
  content publiceren maar niet pushen. Per aankondiging, activiteit of video kan hoogstens één keer een melding gaan
  (unieke sleutel op `content_type, content_id`). Alleen gepubliceerde content komt in aanmerking: geen verlopen
  aankondiging en geen activiteit die al voorbij is. Titel en tekst zijn optioneel aan te passen (100 en 250 tekens);
  leeg betekent een standaardtekst.
- **Waarom:** Een pushmelding bereikt iedereen direct en is niet terug te halen. Eén keer per item voorkomt dubbel
  versturen door een dubbele klik of twee beheerders tegelijk.
- **Gevolg:** Het portaal toont vooraf het bereik (volgers en toestellen), vraagt bevestiging en laat daarna het
  resultaat zien in plaats van de verstuurknop.

## D-034 — Een melding die halverwege stopt, blijft "wordt verstuurd"

- **Status:** Besloten (fase 5); in fase 6 opgelost door D-041
- **Besluit:** Verzenden gebeurt na de commit, op de achtergrond. Stopt de backend precies tijdens het verzenden, dan
  blijft de melding op SENDING staan en kan hij (door D-033) niet opnieuw worden verstuurd.
- **Waarom:** Automatisch opnieuw versturen kan dubbele meldingen geven; dat is erger dan een gemiste melding. Een
  herstelmechanisme hoort bij de productievoorbereiding.
- **Gevolg:** Op de lijst voor fase 6: een herstelactie voor platformbeheerders, of markeren als mislukt na een
  time-out.

## D-035 — Providers voor e-mail en push: `none`, echt, of `log`

- **Status:** Besloten (fase 5)
- **Besluit:** `MAIL_PROVIDER` is `none`, `smtp` of `log`; `PUSH_PROVIDER` is `none`, `fcm` of `log`. Standaard is
  `none`: wachtwoordherstel geeft dan `503 PASSWORD_RESET_UNAVAILABLE` en verzenden `503`. Alleen het profiel `local`
  gebruikt `log`; dat schrijft e-mails en meldingen naar het log, waarschuwt bij het starten dat er niets wordt
  verstuurd, en de backend meldt `simulated: true` zodat het portaal "Testomgeving" toont.
- **Waarom:** Geen nepintegraties en geen geheimen in de repository, maar wel lokaal te testen. Een productie zonder
  instellingen zegt eerlijk dat iets niet kan, in plaats van te doen alsof het gelukt is.
- **Gevolg:** De echte SMTP- en FCM-koppeling zijn alleen tegen lokale testdubbels getest (zie het implementatieplan).

## D-036 — Wachtwoordherstel met een code per e-mail

- **Status:** Besloten (fase 5)
- **Besluit:** `POST /auth/password-reset` stuurt een code van 8 tekens (zonder verwarrende tekens zoals 0/O en 1/I,
  getoond als ABCD-EFGH), 30 minuten geldig, hoogstens 5 pogingen. De database bewaart alleen een SHA-256-hash van
  gebruiker plus code. Het antwoord is altijd `202`, ook voor een onbekend adres; per adres gaan hoogstens 3 e-mails
  per uur, daarboven wordt stil niets verstuurd. Een nieuwe aanvraag maakt oudere codes ongeldig. Bevestigen zet het
  nieuwe wachtwoord, beëindigt alle sessies en logt in op het huidige toestel.
- **Waarom:** Een code werkt in de app zonder deep links of universal links (die per moskee-app een eigen domein en
  configuratie vragen). Het altijd gelijke antwoord verraadt niet welke adressen een account hebben.
- **Gevolg:** Per IP gelden limieten op aanvragen en bevestigen; samen met 5 pogingen per code maakt dat raden
  onhaalbaar.

## D-037 — Account zelf verwijderen

- **Status:** Besloten (fase 5)
- **Besluit:** `POST /api/v1/me/deletion` met het wachtwoord verwijdert het account definitief. De database ruimt via
  foreign keys mee op: sessies, volgen en voorkeuren, pushtoestellen, herstelcodes en aanmeldingen voor activiteiten
  (migratie V12). Een account dat een moskee beheert of content heeft geschreven, kan dat niet zelf (`409
  ACCOUNT_DELETION_BLOCKED`); dat regelt een platformbeheerder.
- **Waarom:** De app-stores en de AVG verwachten dat een gebruiker zijn account zelf kan verwijderen. Content van een
  moskee mag niet ongemerkt zijn auteur verliezen.
- **Gevolg:** Wachtwoord wijzigen en account verwijderen hebben samen een limiet per IP (10 per 15 minuten), zodat een
  gestolen toegangstoken niet gebruikt kan worden om het wachtwoord te raden.

## D-038 — FCM zonder Firebase Admin SDK

- **Status:** Besloten (fase 5)
- **Besluit:** De backend praat met de FCM HTTP v1-API via de bestaande `RestClient`. Het toegangstoken komt van een
  serviceaccount (JSON-bestand, pad in `FCM_CREDENTIALS_FILE`) via een ondertekende JWT (RFC 7523), ondertekend met
  Nimbus, dat al in het project zit. Het token wordt hergebruikt tot kort voor het verloopt.
- **Waarom:** De Admin SDK trekt veel afhankelijkheden mee (gRPC, Google Cloud-bibliotheken) voor één HTTP-aanroep.
- **Gevolg:** De koppeling is getest tegen een lokale stub die de handtekening van de JWT controleert en
  FCM-antwoorden nabootst, niet tegen Google zelf.

## D-039 — Pushmeldingen in de app

- **Status:** Besloten (fase 5)
- **Besluit:** De app gebruikt `firebase_core` en `firebase_messaging`. De Firebase-instellingen komen per build uit
  `--dart-define` (geen `google-services.json` of `GoogleService-Info.plist` in de repository). Zonder die instellingen,
  en op het web, zijn meldingen uit en zegt de app dat bij de meldingsvoorkeuren. Toestemming wordt pas gevraagd als de
  gebruiker de moskee gaat volgen of een onderwerp aanzet, niet bij het starten. Het toestel wordt aangemeld na inloggen
  (als er toestemming is) en afgemeld vóór uitloggen. Tikken op een melding opent de aankondiging, activiteit of
  lezing; een melding terwijl de app open is, verschijnt onderin met een knop "Bekijken".
- **Waarom:** De Firebase-instellingen verschillen per moskee-app (white-label; aanvankelijk een eigen project per
  app, in fase 7 herzien naar één project met een app per moskee, zie D-049). Toestemming vragen met een
  duidelijke aanleiding geeft meer "ja" en is wat Apple en Google aanraden.
- **Gevolg:** Verloopt een sessie zonder uitloggen, dan blijft het toestel aangemeld tot de volgende keer inloggen op
  dat toestel (het token gaat dan naar de nieuwe gebruiker) of tot FCM het token ongeldig meldt. Voor iOS moeten in
  Xcode de capability Push Notifications en een APNs-sleutel in Firebase worden ingesteld; dat kan alleen met een
  Apple Developer-account en is niet gedaan.

## D-040 — Bewaartermijnen worden automatisch toegepast

- **Status:** Besloten (fase 6); termijnen voor aanmeldingen en pushtokens zijn een voorstel aan de moskee
- **Besluit:** De backend ruimt elke nacht om 03:30 (Europe/Amsterdam) op: verlopen sessies 7 dagen na verlopen,
  herstelcodes 1 dag na verlopen, aanmeldingen 365 dagen na afloop van de activiteit (de activiteit zelf blijft),
  pushtokens die 270 dagen niet zijn gezien. Alle termijnen zijn instelbaar (`RETENTION_*`); opschonen kan uit.
- **Waarom:** De AVG vraagt om niet langer te bewaren dan nodig. Een geplande taak in de backend werkt zonder extra
  infrastructuur. De korte marge na verlopen helpt bij het uitzoeken van een melding over een sessie.
- **Gevolg:** De periodes staan met hun reden in PRIVACY.md. De termijnen van 365 en 270 dagen zijn gekozen
  standaarden die de moskee moet bevestigen. Draaien meerdere backends tegelijk, dan ruimen ze dubbel op; dat is
  onschuldig (een tweede delete vindt niets).

## D-041 — Een melding die blijft hangen, wordt na 30 minuten mislukt

- **Status:** Besloten (fase 6); lost D-034 op
- **Besluit:** Elke 5 minuten zoekt de backend meldingen die langer dan 30 minuten (`RETENTION_PUSH_SENDING_TIMEOUT`)
  op SENDING staan, en markeert ze als FAILED. Alle nog niet afgeleverde toestellen tellen als mislukt. Ze worden niet
  automatisch opnieuw verstuurd.
- **Waarom:** Opnieuw versturen kan dubbele meldingen geven (zie D-033); een eerlijke status "mislukt" laat de
  beheerder zelf beslissen, bijvoorbeeld met een nieuwe aankondiging. Verzenden duurt bij pilotaantallen seconden, dus
  30 minuten is ruim.
- **Gevolg:** Het portaal toont na een herstart van de backend binnen 35 minuten "mislukt" in plaats van eeuwig
  "wordt verstuurd".

## D-042 — Beveiligingsheaders en Content-Security-Policy

- **Status:** Besloten (fase 6)
- **Besluit:** De API stuurt naast de standaardheaders van Spring Security (`nosniff`, `X-Frame-Options: DENY`,
  `no-store`, HSTS over HTTPS) `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` en
  `Referrer-Policy: no-referrer`; Swagger UI (alleen lokaal) is uitgezonderd. Het portaal (nginx) stuurt een CSP die
  alleen eigen scripts en stijlen toestaat met een nonce per verzoek (`ngCspNonce`, nginx `$request_id`), API-verkeer
  alleen naar `API_BASE_URL`, afbeeldingen ook via https (thumbnails), geen frames en geen plugins.
- **Waarom:** Een CSP zonder `unsafe-inline` maakt XSS veel moeilijker. Angular voegt component-stijlen in met de nonce
  en de build zet de nonce ook op de kritieke CSS, dus er is geen `unsafe-inline` nodig.
- **Gevolg:** `EndpointSecurityIT` controleert de API-headers; de portaalheaders zijn met Playwright tegen de container
  gecontroleerd (geen CSP-fouten). Komt er een externe bron bij (bijvoorbeeld een kaart), dan moet de CSP in
  `frontend/webportaal/docker/security-headers.inc.template` mee.

## D-043 — Ontwikkelinstellingen weigeren buiten `local` en `test`

- **Status:** Besloten (fase 6)
- **Besluit:** `MAIL_PROVIDER=log` en `PUSH_PROVIDER=log` schrijven herstelcodes en meldingen naar het log. Buiten de
  profielen `local` en `test` weigert de backend te starten met deze instellingen (`LocalOnly`).
- **Waarom:** Een herstelcode in een productielog is een lek; een vergeten instelling mag dat niet veroorzaken.
- **Gevolg:** Fout instellen valt direct op bij het starten in plaats van later in de logs.

## D-044 — Containers en één server voor de pilot

- **Status:** Besloten (fase 6)
- **Besluit:** Backend en portaal krijgen elk een Docker-image (meertraps, draaiend zonder rootrechten, met
  healthcheck). `deploy/docker-compose.prod.yml` draait PostgreSQL, backend en portaal op één server achter een
  TLS-reverse proxy op de host; poorten alleen op 127.0.0.1. Back-ups met `pg_dump` (custom-formaat) en rotatie;
  herstel altijd eerst in een nieuwe database. Rollback via de vorige image-tag, of bij een nieuwe migratie via een
  forward fix of herstel van de back-up (Flyway gaat alleen vooruit).
- **Waarom:** Voor één moskee is Kubernetes of een managed platform onnodig complex en duur. Docker Compose is bekend,
  herhaalbaar en verhuist later zonder wijzigingen in de images. Eén portaalimage voor elke omgeving omdat
  `config.json` bij het starten wordt gemaakt.
- **Gevolg:** De mailhealthcheck staat uit, zodat een SMTP-storing de API niet als `DOWN` meldt. CI bouwt beide images
  en test de stack. Hosting zelf is nog niet gekozen (productieblokkade, DEPLOYMENT.md).

## D-045 — Pushtoken pas na toestemming

- **Status:** Besloten (fase 6); niet op een echt toestel getest
- **Besluit:** Automatisch initialiseren van Firebase Messaging staat uit in het Android-manifest en `Info.plist`. De
  app zet het aan op het moment dat ze een token ophaalt, en dat doet ze alleen na toestemming.
- **Waarom:** Standaard maakt Firebase Messaging al bij het starten een token en praat het met Google, ook als de
  gebruiker nooit meldingen wil. Dat past niet bij "alleen wat nodig is".
- **Gevolg:** Te controleren zodra er een Firebase-project is (fase 6 open punt, zie het implementatieplan).

## D-046 — Merkconfiguratie per moskee in `brands/<slug>/`

- **Status:** Besloten (fase 7)
- **Besluit:** Alles wat per moskee-app verschilt staat in `brands/<slug>/brand.json` (naam, ID's, kleurschalen,
  links, backendadres) plus het beeldmerk `mark.svg`. Een JSON Schema beschrijft het formaat; een eigen, kleine
  controle in `tools/brand/lib.mjs` past het toe (alleen de sleutelwoorden die het schema gebruikt; een onbekend
  sleutelwoord is een fout) en controleert ook het beeldmerk, dubbele ID's tussen merken en het contrast.
- **Waarom:** Eén plek met duidelijke standaardwaarden die een niet-ontwikkelaar kan lezen. Geen JSON Schema-library:
  het schema is klein, en zo draaien de controles met alleen Node, net als de tokengenerator.
- **Gevolg:** Contactgegevens, beschrijving en tijdzone staan niet in het merk maar in de backend (D-050). Nieuwe
  velden vragen een aanpassing van schema, controle en generator samen.

## D-047 — Merk toepassen met codegeneratie in plaats van Flutter-flavors

- **Status:** Besloten (fase 7)
- **Besluit:** `node tools/brand/brand.mjs apply <slug>` schrijft de merkafhankelijke bestanden (Dart-constanten,
  `brand.properties` voor Gradle, `Brand.xcconfig` voor Xcode, tokens, logo's, iconen, portaalnaam). De repository
  bevat altijd het standaardmerk uit `brands/index.json`; CI controleert dat (`brand.mjs check`) en bouwt in een
  matrix elke map in `brands/`.
- **Waarom:** Flavors vragen per moskee extra blokken in Gradle, extra schemes en targets in Xcode en iconensets per
  flavor; dat groeit met elke moskee en is foutgevoelig om met de hand te onderhouden. Met generatie blijft het
  project gelijk en is een nieuwe moskee alleen een map met data.
- **Gevolg:** Er is per checkout één merk actief. Om lokaal een andere app te draaien pas je dat merk toe en daarna
  weer het standaardmerk vóór het committen (CI faalt anders). Een testmoskee (`voorbeeldmoskee`) bewaakt dat de
  generatie voor een tweede merk blijft werken.

## D-048 — Paletnamen primary en accent; typografie centraal

- **Status:** Besloten (fase 7)
- **Besluit:** De paletgroepen `green` en `gold` heten `primary` en `accent` en komen uit het merk; een merk kan ook
  de neutrale tinten vervangen. De donkere tinten van de donkere modus draaien hun kleurtoon mee met de merkkleur
  (voor het pilotmerk blijven ze exact gelijk). Status- en neutrale kleuren, maten en typografie blijven centraal.
- **Waarom:** De namen beschreven Noeroel Islam, niet de rol. Typografie per moskee vraagt per font een licentie
  die meeleveren in apps toestaat en een eigen controle op leesbaarheid; dat is nu niet nodig.
- **Gevolg:** Code gebruikt `TokPalette.primary*` en `accent*`. Een ander font per moskee is een uitbreiding
  (WHITE_LABEL.md, "Uitbreiden").

## D-049 — Eén Firebase-project; pushtokens per app

- **Status:** Besloten (fase 7); herziet D-039 op het punt "eigen Firebase-project per app"
- **Besluit:** Het platform heeft één Firebase-project met per moskee een Android- en een iOS-app. De backend stuurt
  met één serviceaccount. Een pushtoken wordt aangemeld met de slug van de moskee van de app
  (`PUT /me/devices {token, platform, mosqueSlug}`) en bewaard met die moskee (`push_device.mosque_id`, V13). Een
  melding van een moskee gaat alleen naar tokens van haar eigen app.
- **Waarom:** Eén backend met een serviceaccount per moskee zou de configuratie per moskee laten groeien. Zonder
  koppeling aan de app zou iemand die twee moskeeën volgt de meldingen van moskee B in de app van moskee A krijgen.
- **Gevolg:** Tokens van vóór V13 krijgen de moskee als er precies één moskee is (de pilot); anders blijven ze leeg en
  worden ze niet aangeschreven tot de app opnieuw aanmeldt (bij elke start). Er wordt niets verwijderd.

## D-050 — Eén backend en één portaal voor alle moskeeën

- **Status:** Besloten (fase 7)
- **Besluit:** Alle apps praten met dezelfde backend; scheiding per moskee zit in de backend (D-006, D-017, getest in
  `TenantIsolationIT`). Het beheerportaal is één installatie; een platformbeheerder voegt er moskeeën toe (*Moskee
  toevoegen*, dezelfde slug als het merk). Contactgegevens, adres en beschrijving beheert de moskee in het portaal;
  categorieën zijn voor alle moskeeën gelijk.
- **Waarom:** Eén installatie is één ding om te beveiligen, back-uppen en bij te werken. Gegevens die vaak wijzigen
  horen niet in een app-build.
- **Gevolg:** Het portaal toont de naam en kleuren van het standaardmerk. Bij meer moskeeën een eigen platformmerk
  voor het portaal overwegen (WHITE_LABEL.md).

## D-051 — Ondertekening buiten de repository; releases alleen handmatig

- **Status:** Besloten (fase 7)
- **Besluit:** Gradle tekent een release alleen met een sleutel uit omgevingsvariabelen. Sleutels en Firebase-
  waarden per app staan in een GitHub Environment met de naam van de slug. De workflow Release-app start alleen met
  de hand, bouwt en tekent, en levert een artifact; hij uploadt niet naar de stores. CI tekent met een wegwerpsleutel
  die alleen tijdens de job bestaat.
- **Waarom:** Publiceren is niet terug te draaien en vraagt accounts, rechten en controles die er nog niet zijn. Een
  Environment met verplichte goedkeuring houdt sleutels buiten pull requests van anderen.
- **Gevolg:** iOS wordt ongetekend gebouwd tot er een Apple Developer-account is. Uploaden gebeurt met de hand na de
  releasecontrole in WHITE_LABEL.md.
