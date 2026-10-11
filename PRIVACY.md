# Privacy en AVG

Dit document beschrijft welke persoonsgegevens het platform verwerkt, waarom, wie erbij kan, hoe lang ze bewaard
blijven en hoe ze worden verwijderd. Het is de technische basis voor de privacyverklaring en het
verwerkingsregister van de moskee. Het is geen juridisch advies: de punten onder "Open punten" moeten met de moskee
(en waar nodig een jurist of privacyadviseur) worden besloten voordat de app live gaat.

Stand: fase 6 (11 oktober 2026). Bij elke wijziging in de gegevens die het platform opslaat, wordt dit document
bijgewerkt.

## Rollen

- **Verwerkingsverantwoordelijke:** de moskee (voor de pilot Noeroel Islam). Zij bepaalt waarvoor de app is en wat
  erin komt.
- **Verwerker:** de partij die het platform Mijn Ummah host en beheert. Hiervoor is een verwerkersovereenkomst
  met de moskee nodig (zie "Open punten").
- **Subverwerkers:** zie "Externe diensten".

## Uitgangspunten

- **Alleen wat nodig is.** Bezoekers gebruiken de app zonder account: agenda, lezingen, aankondigingen en
  moskeegegevens zijn openbaar. Een account is alleen nodig om te volgen, meldingen te krijgen of zich aan te melden
  voor een activiteit. Bij registratie vragen we alleen e-mailadres, naam en wachtwoord.
- **Geen tracking.** Geen analytics, geen advertentie-ID's, geen crashrapportage naar derden, geen cookies voor
  authenticatie. De app en het portaal laden geen scripts of fonts van derden. Firebase wordt alleen gebruikt voor
  het afleveren van pushmeldingen (`firebase_messaging`), zonder Firebase Analytics.
- **Geen IP-adressen in de database.** De limieten per IP (tegen raden van wachtwoorden) houden tellers alleen in
  het geheugen bij, een kwartier tot een uur.
- **Toegang op basis van rol en moskee.** Elke beheeractie wordt per moskee gecontroleerd in de applicatielaag en
  getest (`TenantIsolationIT`, `EndpointSecurityIT`). Een beheerder van moskee A ziet niets van moskee B.
- **Geen niet-openbare gegevens naar buiten.** Openbare API-routes tonen alleen gepubliceerde content van actieve
  moskeeën; content met zichtbaarheid "geregistreerde gebruikers" alleen aan ingelogde gebruikers. Namen en
  e-mailadressen van gebruikers staan nooit in een openbaar antwoord.

## Gegevensoverzicht

| Gegeven | Waar | Doel | Grondslag (voorstel) | Wie kan erbij | Bewaartermijn | Verwijderen |
|---|---|---|---|---|---|---|
| E-mailadres, naam, wachtwoordhash (BCrypt), rol, status | `app_user` | Inloggen, account beheren, herstel-e-mail | Overeenkomst (art. 6 lid 1b) | De gebruiker zelf; platformbeheerder (accountbeheer); moskeebeheerder ziet naam en e-mail alleen bij beheerders en bij aanmeldingen voor eigen activiteiten | Zolang het account bestaat | Gebruiker verwijdert zelf in de app (`POST /me/deletion`); of platformbeheerder |
| Sessies (hash van refresh token) | `refresh_token` | Ingelogd blijven, uitloggen op alle toestellen | Overeenkomst | Niemand leest ze; alleen de backend vergelijkt hashes | Geldig 30 dagen; verlopen sessies worden 7 dagen daarna automatisch verwijderd | Uitloggen, wachtwoord wijzigen, account verwijderen, nachtelijke opschoning |
| Herstelcodes (hash) | `password_reset_code` | Wachtwoord vergeten | Overeenkomst | Niemand leest ze | Geldig 30 minuten; 1 dag na verlopen automatisch verwijderd | Nachtelijke opschoning; met het account |
| Volgen en meldingsvoorkeuren | `mosque_follow`, `notification_preference` | Bepalen wie welke melding krijgt | Toestemming (art. 6 lid 1a), zie "Open punten" over art. 9 | De gebruiker; moskeebeheerder ziet alleen aantallen | Tot ontvolgen of account verwijderen | Ontvolgen in de app; met het account |
| Pushtoken en platform (Android/iOS) | `push_device` | Meldingen afleveren | Toestemming (toestemmingsvraag van het besturingssysteem) | Niemand leest ze; de backend stuurt ze naar FCM | Tot uitloggen, account verwijderen, FCM meldt het token als ongeldig, of 270 dagen niet meer gezien | Uitloggen, toestemming intrekken + uitloggen, nachtelijke opschoning |
| Aanmeldingen voor activiteiten | `event_registration` | Plaatsen beheren, beheerder weet wie komt | Overeenkomst | De gebruiker; ADMIN van die moskee (naam en e-mail in de aanmeldlijst) | Tot 365 dagen na afloop van de activiteit | Afmelden in de app; met het account; nachtelijke opschoning |
| Beheerdersrollen | `mosque_administrator` | Wie mag wat beheren | Gerechtvaardigd belang / overeenkomst met de moskee | Platformbeheerder, ADMIN van die moskee | Tot de rol wordt ingetrokken | ADMIN of platformbeheerder |
| Auteur van content (`created_by`) | `event`, `media_item`, `announcement`, `push_notification`, `mosque_administrator` | Verantwoording: wie heeft wat gepubliceerd of verstuurd | Gerechtvaardigd belang | Beheerders van die moskee (niet openbaar) | Zolang de content bestaat | Een account met content kan niet zelf worden verwijderd; platformbeheerder draagt over of verwijdert de content eerst (D-037) |
| Verzonden pushmeldingen (titel, tekst, aantallen) | `push_notification` | Status tonen, dubbel versturen voorkomen | Gerechtvaardigd belang | Beheerders van die moskee | Zolang de content bestaat (geen persoonsgegevens behalve de auteur) | Met de moskee |
| Lokaal op het toestel: sessie en profiel | Beveiligde opslag (Keychain / Android Keystore) | Ingelogd blijven | Overeenkomst | Alleen de app | Tot uitloggen | Uitloggen, app verwijderen |
| Lokaal op het toestel: offline-kopie van openbare lijsten, themakeuze | `shared_preferences` | Werken zonder verbinding | Gerechtvaardigd belang | Alleen de app | Tot uitloggen (cache) of app verwijderen | Uitloggen wist de cache |
| Back-ups van de database | Back-upmap op de server en een externe kopie | Herstel na storing of fout | Gerechtvaardigd belang | Alleen de beheerder van de server | 30 dagen (instelbaar, `BACKUP_KEEP_DAYS`) | Oudere back-ups worden automatisch verwijderd; een verwijderd account verdwijnt dus na uiterlijk 30 dagen ook uit de back-ups |

De bewaartermijnen voor sessies, herstelcodes, aanmeldingen en pushtokens worden elke nacht (03:30 Europe/Amsterdam)
toegepast door de backend (`IdentityRetention`, `RegistrationRetention`, `PushRetention`) en zijn instelbaar met
`RETENTION_*` (zie `backend/.env.example`). De termijn van 365 dagen voor aanmeldingen en 270 dagen voor pushtokens
is een voorstel; de moskee kan ze aanpassen zonder codewijziging.

## Toestemming voor pushmeldingen

- De app vraagt pas toestemming als de gebruiker een moskee gaat volgen of een onderwerp aanzet, niet bij het
  starten (D-039).
- Vóór die toestemming maakt de app geen pushtoken aan: automatisch initialiseren van Firebase Messaging staat uit
  (`firebase_messaging_auto_init_enabled` in het Android-manifest, `FirebaseMessagingAutoInitEnabled` in
  `Info.plist`) en wordt pas aangezet als er toestemming is. Dit is nog niet op een echt toestel gecontroleerd (geen
  Firebase-project beschikbaar).
- De gebruiker zet per onderwerp (aankondigingen, activiteiten, lezingen) meldingen aan of uit in de app, en kan de
  toestemming op elk moment intrekken in de instellingen van het toestel. Ontvolgen of uitloggen meldt het toestel
  af bij de backend.
- Alleen een ADMIN van de moskee kan een melding versturen, alleen over gepubliceerde content, één keer per item
  (D-033). Meldingen bevatten de titel en een korte tekst, geen persoonsgegevens.

## Logging

- De backend logt geen e-mailadressen, wachtwoorden, tokens, herstelcodes of inhoud van verzoeken. Fouten bij het
  versturen van e-mail loggen alleen het soort fout. Startmeldingen noemen de instelling
  (`BOOTSTRAP_ADMIN_EMAIL`), niet het adres.
- Wat wel gelogd wordt: start en stop, migraties, aantallen (bijvoorbeeld "3 verlopen sessies verwijderd",
  "notificatie X: 12 afgeleverd"), en technische UUID's van content.
- De providers `log` voor e-mail en push schrijven herstelcodes en meldingen wel naar het log. Ze zijn bedoeld voor
  een ontwikkelmachine: buiten de profielen `local` en `test` weigert de backend te starten met deze instelling
  (`LocalOnly`).
- In productie logt de backend één JSON-object per regel (ECS-formaat). Docker bewaart per container hoogstens 5
  bestanden van 10 MB. De nginx van het portaal logt verzoeken met IP-adres; de reverse proxy op de server ook. Advies:
  hoogstens 14 dagen bewaren (zie `DEPLOYMENT.md`).

## Beveiliging (samengevat)

- Wachtwoorden met BCrypt (minimaal 12 tekens), refresh tokens en herstelcodes alleen als hash opgeslagen,
  roterende refresh tokens met detectie van hergebruik, limieten op inloggen en herstellen.
- Alle verbindingen via HTTPS (reverse proxy, zie `DEPLOYMENT.md`); de database is niet bereikbaar van buiten de
  server.
- Beveiligingsheaders op API en portaal, waaronder een strikte Content-Security-Policy (D-042).
- Geheimen staan alleen in `.env` op de server en in gekoppelde bestanden (`secrets/`), nooit in de repository.

## Beeld- en publicatierechten

- Het portaal wijst bij de afbeelding van een activiteit en bij een video op beeldrechten en toestemming van
  herkenbare personen.
- Video's worden niet opgeslagen: het platform bewaart alleen een YouTube-link. De moskee is verantwoordelijk voor
  de rechten op de video en de toestemming van spreker en zichtbare personen.
- In de app speelt de video af via `youtube-nocookie.com` (privacy-enhanced mode). Bij afspelen maakt het toestel wel
  verbinding met YouTube (Google); dat staat in de privacyverklaring.

## Externe diensten

| Dienst | Waarvoor | Gegevens | Status |
|---|---|---|---|
| Hosting (nog te kiezen, bij voorkeur in de EU) | Backend, database, portaal, back-ups | Alles hierboven | Nog niet gekozen |
| Google Firebase Cloud Messaging | Pushmeldingen afleveren | Pushtoken, titel en tekst van de melding | Gebouwd, niet getest met echte credentials. Doorgifte naar de VS (EU-US Data Privacy Framework) in de privacyverklaring noemen |
| Apple Push Notification service (via FCM) | Pushmeldingen op iOS | Idem | Nog niet ingericht |
| SMTP-dienst (nog te kiezen) | Herstel-e-mail | E-mailadres, herstelcode | Gebouwd, niet getest met een echte provider |
| YouTube (privacy-enhanced) | Video's afspelen in de app | IP-adres en toesteldata bij afspelen | In gebruik |

## Rechten van betrokkenen

| Recht | Hoe |
|---|---|
| Verwijdering | In de app: Account → Account verwijderen. Verwijdert account, sessies, volgen, voorkeuren, pushtokens, herstelcodes en aanmeldingen direct. Beheerders en auteurs via de platformbeheerder |
| Rectificatie | Naam en wachtwoord: de gebruiker in de app (wachtwoord) of de platformbeheerder; e-mailadres wijzigen kan nog niet zelf |
| Inzage en dataportabiliteit | Nog geen knop. Op verzoek stelt de platformbeheerder een overzicht samen uit de tabellen hierboven (handmatig, binnen een maand) |
| Bezwaar en intrekken van toestemming | Meldingen uitzetten per onderwerp, ontvolgen, toestemming intrekken in het toestel |

## Open punten (voor livegang)

1. **Bijzondere persoonsgegevens (art. 9 AVG).** Dat iemand een moskee volgt of zich aanmeldt voor een activiteit kan
   iets zeggen over godsdienst. De moskee moet besluiten op welke uitzondering zij zich beroept (bijvoorbeeld art. 9
   lid 2 sub a, uitdrukkelijke toestemming, of sub d, een organisatie op godsdienstig gebied voor personen met wie zij
   regelmatig contact heeft) en dit in de privacyverklaring opnemen. Juridisch advies aanbevolen.
2. **Privacyverklaring** schrijven en publiceren op een vaste URL; die URL is nodig voor de App Store en Google Play
   (inclusief de "Data safety"- en "App Privacy"-formulieren) en hoort in de app bij Profiel.
3. **Verwerkersovereenkomst** tussen moskee en platformbeheerder, en met de hostingpartij en SMTP-dienst.
4. **Verwerkingsregister** van de moskee aanvullen met het overzicht hierboven.
5. **Inactieve accounts:** nog geen automatische verwijdering. Voorstel: na 2 jaar zonder inloggen een e-mail en
   daarna verwijderen; daarvoor is een kolom "laatst ingelogd" nodig.
6. **Inzage en export** zijn handmatig; een exportfunctie kan in een latere fase.
7. **Beveiligingsincidenten:** procedure voor datalekken (melden bij de Autoriteit Persoonsgegevens binnen 72 uur)
   vastleggen bij de moskee.
