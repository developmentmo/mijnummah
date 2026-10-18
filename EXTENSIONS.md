# Latere uitbreidingen

Drie uitbreidingen staan op de lijst maar worden pas gebouwd na expliciete toestemming: officieel lidmaatschap,
live-streaming en betalingen. Dit document beschrijft per uitbreiding waar de huidige architectuur haar ruimte geeft,
wat er dan nieuw nodig is, wat er eerst geregeld moet zijn en welke risico's er zijn. Er is bewust **niets** van
gebouwd: geen lege modules, tabellen, schermen of abstracties (D-011).

## Wat er al is om op voort te bouwen

| Bouwsteen | Waar | Waarom het helpt |
|---|---|---|
| Modules met vaste grenzen (Spring Modulith) | `backend/src/main/java/nl/mijnummah/*`, gecontroleerd door `ModularityTest` | Een uitbreiding wordt een eigen module die alleen de publieke API van andere modules gebruikt (`MosqueAccessService`, `MosqueDirectory`, `UserDirectory`, `EventDirectory`, `FollowerDirectory`) |
| Moskee als tenant | `mosque_id` op elke moskeetabel, `MosqueAccessService`, `TenantIsolationIT` | Nieuwe tabellen volgen hetzelfde patroon; de isolatietest breidt mee uit |
| Rechten per moskee | `MosqueRole` (ADMIN, EDITOR) en `MosquePermission` | Een nieuw recht (bijv. leden beheren) is een extra `MosquePermission`, niet een nieuw rechtensysteem |
| Volgen is geen lidmaatschap | `follow`-module, D-011 | Lidmaatschap kan erbij komen zonder volgers te veranderen of te verwarren |
| Video bij een externe dienst | `VideoProvider` in de `media`-module (YouTube, D-009) | Een livestream is ook een video bij een externe dienst; geen eigen videoserver nodig |
| Pushmeldingen per onderwerp | `PushContentType` en `NotificationTopic` | Een nieuw onderwerp (bijv. "live") is een extra waarde met een eigen voorkeur |
| Bewaartermijnen | `common/retention`, D-040 | Nieuwe persoonsgegevens krijgen een eigen termijn in dezelfde opschoning |
| Merk per moskee | `brands/<slug>/`, WHITE_LABEL.md | Per moskee aan of uit zetten hoort in de backend (moskee-instelling), niet in het merk, zodat het zonder nieuwe app-versie kan |

## Officieel lidmaatschap

**Doel:** mensen kunnen lid worden van een moskee; het bestuur keurt aanvragen goed, houdt een ledenlijst bij en
kan later contributies koppelen.

**Waar het past**
- Een nieuwe module `membership`, naast `follow`. Volgen blijft wat het is: meldingen krijgen. Lid zijn is een
  formele relatie met de moskee met eigen gegevens en een eigen status.
- Gebruikt `UserDirectory` (wie), `MosqueDirectory` en `MosqueAccessService` (welke moskee, wie mag beheren).
- Nieuwe permissie, bijvoorbeeld `MANAGE_MEMBERS`, standaard alleen voor ADMIN.

**Wat er dan nieuw nodig is**
- Tabellen (via een Flyway-migratie): lidmaatschap per (moskee, gebruiker) met status (`AANGEVRAAGD`, `ACTIEF`,
  `GEWEIGERD`, `BEËINDIGD`), datums en wie besliste; alleen de velden die het bestuur echt nodig heeft (naam en
  e-mail staan al bij het account; adres of geboortedatum alleen als de statuten dat eisen).
- App: lid worden vanaf Profiel, eigen status zien, lidmaatschap opzeggen.
- Portaal: aanvragen goedkeuren of weigeren, ledenlijst, export voor de ledenadministratie.
- Tests: aanvraag en besluit, alleen ADMIN ziet leden, leden van moskee A nooit zichtbaar voor moskee B.

**Eerst regelen**
- **Art. 9 AVG.** Lid zijn van een moskee zegt iets over godsdienst: dat is een bijzonder persoonsgegeven. De moskee
  moet de grondslag vastleggen (waarschijnlijk art. 9 lid 2 sub d, verwerking door een religieuze organisatie over
  haar leden) en de ledenlijst mag niet buiten de moskee komen. Zie ook het open punt in PRIVACY.md.
- Wat de statuten van de moskee over lidmaatschap zeggen: wie mag lid worden, wie beslist, wat er bij opzeggen
  gebeurt.
- Bewaartermijn na beëindiging (bijvoorbeeld wat de wet voor de administratie van contributies eist).

**Risico's**
- Een gelekte ledenlijst raakt mensen persoonlijk; strengere toegang en logging van wie de lijst bekijkt of
  exporteert zijn nodig.
- Verwarring tussen volgen en lid zijn; teksten in app en portaal moeten het verschil steeds benoemen.

## Live-streaming

**Doel:** geplande livestreams (vrijdagpreek, lezingen) in de app bekijken, zien of een stream live is, achteraf
terugkijken en een melding krijgen als een stream begint.

**Waar het past**
- In de `media`-module, via `VideoProvider`. Een YouTube-livestream heeft een gewoon video-ID en speelt in dezelfde
  speler (`youtube_player_iframe` in de app); na afloop is dezelfde video de opname. Er komt dus geen eigen
  streamingserver.
- Een stream hoort vaak bij een activiteit; `media_item` kan al naar een activiteit van dezelfde moskee verwijzen
  (`related_event_id`, met een database-constraint op dezelfde moskee).
- Een nieuw pushonderwerp in `PushContentType` en `NotificationTopic` (bijvoorbeeld "live"), met een eigen voorkeur
  zodat niemand ongevraagd meldingen krijgt.

**Wat er dan nieuw nodig is**
- Migratie: een soort per media-item (`RECORDING` of `LIVE`) en een geplande starttijd; een livestatus (`GEPLAND`,
  `LIVE`, `AFGELOPEN`). De huidige tabel heeft dat nog niet: D-011 noemde een type, maar dat is bewust niet
  toegevoegd zolang het geen doel dient.
- Livestatus bepalen: met de hand in het portaal ("nu live" en "afgelopen"), of automatisch via de YouTube Data API.
  Dat laatste vraagt een API-sleutel en quota, en een periodieke taak in de backend.
- App: blok "Nu live" op Home, aftellen bij een geplande stream, melding bij de start.
- Portaal: stream inplannen, YouTube-link van de livestream invullen, status zetten, melding versturen.

**Eerst regelen**
- Een YouTube-kanaal van de moskee met livestreaming aan (YouTube vereist een geverifieerd kanaal en een wachttijd
  bij de eerste keer).
- Afspraken over beeld en geluid: wie er in beeld komt, toestemming van sprekers en bezoekers, wat er met opnames
  gebeurt (PRIVACY.md, "Beeld- en publicatierechten").
- Een streamingopstelling in de moskee (camera, encoder of OBS, stabiele uploadverbinding).

**Risico's**
- Een andere streamdienst dan YouTube vraagt een nieuwe `VideoProvider` en mogelijk een andere speler; kies er
  één en houd de rest buiten de app.
- Een melding "live" die te laat of na afloop komt, werkt averechts; begin met handmatig starten.

## Betalingen

**Doel:** online contributies (eventueel gekoppeld aan lidmaatschap) en giften, bijvoorbeeld voor een project of
in de ramadan.

**Waar het past**
- Een nieuwe module `payment`, alleen als er een concreet betaaldoel is (contributie of gift). Zij gebruikt
  `UserDirectory` en `MosqueAccessService`, en bij contributies de module `membership`.
- Betalen loopt **altijd** via een betaaldienstverlener met een eigen betaalpagina (in Nederland bijvoorbeeld Mollie,
  Stripe of Adyen, met iDEAL). De app en de backend zien nooit kaart- of bankgegevens: ze maken een betaling aan,
  sturen de gebruiker door en verwerken de bevestiging via een ondertekende webhook.
- Elke moskee ontvangt op haar eigen rekening: een eigen account of sub-account bij de betaaldienst per moskee, niet
  via het platform. Zo blijft het platform buiten de geldstroom.

**Wat er dan nieuw nodig is**
- Tabellen: betaling per moskee met bedrag, doel, status (zoals de betaaldienst die meldt) en het kenmerk van de
  betaaldienst; geen bankgegevens.
- Backend: betaling aanmaken, webhook verwerken (handtekening controleren, idempotent), overzicht per moskee.
- App: betalen vanuit een duidelijke knop, terugkomen in de app met de uitkomst.
- Portaal: overzicht en export van ontvangsten voor de penningmeester.
- Geheimen per moskee (API-sleutel van de betaaldienst) in de geheimenopslag van de server, nooit in de repository.

**Eerst regelen**
- Store-regels: Apple en Google laten giften aan een erkende goede doelen-organisatie toe zonder hun eigen
  betaalsysteem, maar alleen onder voorwaarden (in de iOS-app soms alleen via Safari of Apple Pay). Dit moet per
  store worden nagelezen voordat er iets gebouwd wordt.
- Fiscale kant: is de moskee een ANBI (giftenaftrek), welke bevestiging de gever nodig heeft, hoe lang de
  administratie bewaard moet worden (7 jaar).
- Een contract met de betaaldienst per moskee, met hun eigen controles (KYC) en eisen.
- Een beveiligingsreview van de betaalstroom vóór livegang.

**Risico's**
- Verkeerd toegekende betalingen tussen moskeeën: de webhook moet de moskee uit de betaling halen, nooit uit
  invoer van de gebruiker, en de isolatietest moet ook betalingen dekken.
- Afwijzing door een store als de regels voor giften niet goed gevolgd worden.

## Volgorde en aanpak

1. **Lidmaatschap** eerst als het bestuur het wil: het is de basis voor contributies en vraagt geen externe
   diensten, wel een privacybesluit (art. 9).
2. **Live-streaming** kan los daarvan, zodra de moskee een YouTube-kanaal met livestreaming heeft. Begin met
   handmatige status en meldingen.
3. **Betalingen** als laatste: pas na lidmaatschap (voor contributies), het nalezen van de store-regels en een
   contract met een betaaldienst.

Elke uitbreiding volgt dezelfde werkwijze als de fases 1 t/m 7: eigen module, migratie, tests inclusief
tenantisolatie, documentatie in DECISIONS.md en het implementatieplan, en per moskee aan te zetten.
