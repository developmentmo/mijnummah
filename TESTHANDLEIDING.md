# Testhandleiding pilot

Met deze handleiding test je het platform zoals een echte moskee het gaat gebruiken. Je sluit een moskee aan via het
portaal, de beheerder richt haar in, en je controleert in de app wat bezoekers en volgers zien. Daarna voeg je een
tweede moskee toe en controleer je dat de twee gescheiden blijven. Alles gebeurt via de portalen en de app, zonder
code, configuratie of handmatige databasewijziging (D-061).

Elke stap noemt wat je moet zien. Vink af wat klopt, en noteer wat niet klopt in het feedbackformulier onderaan.

## 1. Voorbereiden

**Lokaal** (vereist Docker): `docker compose up -d --build` in de root. Portaal op http://localhost:4200, API op
http://localhost:8082. Log in het portaal in als platformbeheerder `admin@mijnummah.local` met wachtwoord
`lokaal-wachtwoord-123`. Dat zijn wegwerpgegevens die alleen lokaal bestaan. Er staan nog geen moskeeën klaar.

**App:** `cd frontend/app && flutter run` (Android-emulator, iOS-simulator of desktop). De app vindt de lokale
backend zelf; zie [frontend/app/README.md](frontend/app/README.md).

**Snel testgegevens nodig?** Het script [tools/demo/seed-demo.mjs](tools/demo/README.md) zet twee verzonnen
testmoskeeën met beheerders en content klaar via dezelfde API als het portaal. Voor de echte pilot doe je stap 2 en 3
met de hand in het portaal.

Hulpaccounts: maak voor de test drie e-mailadressen klaar, één voor de beheerder van de moskee, één voor een
redacteur en één voor een gewone bezoeker. Lokaal worden e-mails niet verstuurd: uitnodigingen en herstelcodes staan
in het log van de backend (`docker compose logs backend`), duidelijk gemarkeerd als "niet verstuurd".

## 2. Moskee aansluiten (platformbeheerder, portaal)

| # | Doen | Verwacht |
|---|---|---|
| 2.1 | Platform > Dashboard | Totalen voor moskeeën, accounts en content; bij een lege omgeving nullen |
| 2.2 | Platform > Moskeeën > Moskee registreren. Vul naam, beschrijving, adres, plaats en contact in. Laat de slug leeg | De slug wordt voorgesteld op basis van de naam (kleine letters en streepjes) |
| 2.3 | Vul bij eerste beheerder naam en e-mailadres in van iemand zonder account, en registreer | Melding dat de moskee is aangemaakt en de beheerder is uitgenodigd |
| 2.4 | Zoek de uitnodiging in het log (lokaal) of de mailbox, en open de link | Pagina "Wachtwoord instellen"; na opslaan ben je ingelogd als beheerder van alleen deze moskee |
| 2.5 | Platform > Auditlog | Regels voor het aanmaken van de moskee en het toevoegen van de beheerder |
| 2.6 | Platform > Moskeeën: zet de moskee op inactief en daarna weer op actief | Inactief: de moskee verdwijnt uit de app. Actief: ze is weer te zien |

## 3. Moskee inrichten (beheerder van de moskee, portaal)

| # | Doen | Verwacht |
|---|---|---|
| 3.1 | Moskeegegevens: vul de link naar de gebedstijden in, upload een logo (vierkant) en een omslagfoto (liggend) | Voorbeelden van logo en omslag; een te groot of verkeerd bestand geeft een duidelijke melding |
| 3.2 | Beheerders: voeg de redacteur toe met rol Redacteur | De redacteur ziet de moskee na inloggen en beheert content, maar niet Moskeegegevens of Beheerders, en kan geen pushmeldingen versturen |
| 3.3 | Agenda: maak een openbare activiteit met aanmelden aan en een maximum aantal plaatsen, en een activiteit "alleen voor ingelogde gebruikers". Publiceer beide | Beide staan op gepubliceerd |
| 3.4 | Lezingen en video's: voeg een YouTube-link toe, kies een categorie en publiceer. Plan een tweede video in de toekomst in | De eerste is direct te zien in de app, de ingeplande pas op het gekozen moment |
| 3.5 | Aankondigingen: maak een aankondiging met afbeelding, zet hem op uitgelicht en publiceer | Te zien in de app, bovenaan bij Nieuws |
| 3.6 | Pushmeldingen: stuur een melding over de aankondiging en een eigen bericht aan alle volgers | Het bereik vooraf klopt met het aantal volgers; daarna status en aantallen in de lijst |
| 3.7 | Overzicht | Volgers, komende activiteiten, video's, aankondigingen en recente acties |

## 4. De app (bezoeker en volger)

| # | Doen | Verwacht |
|---|---|---|
| 4.1 | Start de app voor het eerst | Introductie in drie stappen; overslaan kan |
| 4.2 | Home zonder account | Moskeeën ontdekken, de komende activiteiten, nieuwe video's en nieuws van alle moskeeën, en een uitnodiging om in te loggen |
| 4.3 | Ontdekken: zoek op een deel van de naam, filter op plaats | Alleen de passende moskeeën; met een onbekende naam de melding "Geen moskee gevonden" |
| 4.4 | Open het moskeeprofiel | Omslag, logo, beschrijving, knop Gebedstijden, adres en contact, de eigen activiteiten, video's en nieuws |
| 4.5 | Agenda: filter op Vandaag, Deze week, een categorie en de moskee | De lijst past zich aan; de activiteit "alleen voor ingelogde gebruikers" is zonder account niet te zien |
| 4.6 | Maak een account (Profiel > Account aanmaken) en volg de moskee | "Volgend" op de knop; Home toont nu "Jouw moskeeën" en alleen content van gevolgde moskeeën. De app vraagt pas nu om meldingen toe te staan |
| 4.7 | Zet de moskee op favoriet (ster op het profiel) | Ze staat bovenaan bij "Jouw moskeeën" en bij Favorieten |
| 4.8 | Bewaar een video met het hartje en open Profiel > Favorieten | De video staat bij Bewaarde video's |
| 4.9 | Meld je aan voor de activiteit met aanmelden | Bevestiging; bij Profiel > Mijn aanmeldingen; de beheerder ziet je in de aanmeldlijst |
| 4.10 | Profiel > Meldingen na stap 3.6 | De melding van de moskee, ook als de pushmelding het toestel niet bereikte. Tikken opent de aankondiging |
| 4.11 | Profiel: open de moskee, zet een meldingsonderwerp uit en ontvolg daarna | De voorkeur blijft bewaard; na ontvolgen staat de moskee niet meer bij Jouw moskeeën |
| 4.12 | Zet het toestel in vliegtuigmodus en trek Home omlaag | Melding "Je bent offline" met de laatst opgehaalde gegevens |

Pushmeldingen op een echt toestel werken alleen met een Firebase-project (zie DEPLOYMENT.md). Lokaal komen ze in
het log van de backend.

## 5. Tweede moskee zonder code

| # | Doen | Verwacht |
|---|---|---|
| 5.1 | Registreer een tweede moskee in een andere plaats, met een andere beheerder (stap 2.2 t/m 2.4) | Er is niets aan de code, de configuratie of de database veranderd |
| 5.2 | Richt haar kort in (stap 3.1 en 3.3) | |
| 5.3 | Ontdekken in de app | Beide moskeeën; het plaatsfilter toont beide plaatsen |
| 5.4 | Agenda en Nieuws in de app | Content van beide moskeeën, elk met de naam en het logo van de eigen moskee |

## 6. Beveiliging en scheiding van moskeeën

| # | Doen | Verwacht |
|---|---|---|
| 6.1 | Log in het portaal in als beheerder van moskee 1 | Alleen moskee 1 is te kiezen; geen groep Platform in het menu |
| 6.2 | Vervang in de adresbalk het moskee-ID (`/m/<id>/...`) door het ID van moskee 2 (te zien als platformbeheerder) | Melding dat je geen toegang hebt; geen gegevens van moskee 2 |
| 6.3 | Log in als redacteur en open `/m/<id>/beheerders` of `/m/<id>/gegevens` | Geen toegang |
| 6.4 | Log in als gewone bezoeker in het portaal | Geen moskee om te beheren |
| 6.5 | Blokkeer moskee 2 als platformbeheerder en log in als haar beheerder | De moskee is niet meer te beheren en verdwijnt uit de app |
| 6.6 | Blokkeer het account van de redacteur | Inloggen lukt niet meer; een open sessie stopt direct |
| 6.7 | Vraag zes keer achter elkaar met een fout wachtwoord in te loggen | Na een aantal pogingen een melding om het later opnieuw te proberen |

Automatisch getest bij elke wijziging (`./mvnw verify` in `backend/`, ook in CI):

| Test | Wat |
|---|---|
| `PilotOnboardingIT` | Stap 2 t/m 5 via de API: een moskee aansluiten en inrichten, en een tweede moskee die gescheiden blijft |
| `TenantIsolationIT` | De beheerder van moskee A kan via geen enkele route iets van moskee B lezen of wijzigen, ook niet met ID's van B in de routes van A |
| `EndpointSecurityIT` | Elke route behalve de publieke vraagt inloggen; een gebruiker zonder rol komt bij geen enkele beheerroute |
| `TokenSecurityIT` | Vervalste, ongetekende, verlopen of vreemde tokens, en tokens van onbekende of geblokkeerde accounts, komen niet binnen; rechten komen nooit uit het token |
| `AuthFlowIT`, `AuditLogIT`, `MosqueAdminIT`, `PushFlowIT`, `CentralAppIT` | Inloggen en sessies, auditlog, status van moskeeën, pushmeldingen, de centrale app |

## 7. Feedbackformulier

Kopieer dit per bevinding:

```
Stap:            (bijv. 4.6)
Rol:             platformbeheerder / beheerder / redacteur / bezoeker
Toestel:         (bijv. iPhone 13, iOS 18 / Chrome op Windows)
Wat deed je:
Wat verwachtte je:
Wat gebeurde er:
Ernst:           blokkerend / hinderlijk / cosmetisch / idee
Schermafbeelding: ja / nee
```
