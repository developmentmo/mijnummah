# White-label: een app per moskee

Elke moskee krijgt een eigen app in de stores, met eigen naam, logo, kleuren en ID's. Alle apps komen uit dezelfde
code en praten met dezelfde backend. Een nieuwe moskee is een map in `brands/` en een moskee in het portaal, geen
kopie van de code. Beslissingen: D-046 t/m D-051 in [DECISIONS.md](DECISIONS.md).

## Wat per moskee is en wat centraal

| Per moskee (in `brands/<slug>/`, gegenereerd bij het bouwen) | Centraal (voor alle moskeeën gelijk) |
|---|---|
| Naam in de app en onder het icoon (`name`, `launcherName`) | Functionaliteit, schermen en teksten van de app |
| Android application ID en iOS bundle ID | Backend en database (één installatie, gescheiden per moskee) |
| Beeldmerk (`mark.svg`) en daaruit alle logo's, app-iconen en de splash | Beheerportaal (één installatie voor alle moskeeën, zie D-050) |
| Kleurschalen primary en accent (optioneel neutral); de donkere modus volgt de merkkleur | Typografie (Fraunces en Inter, meegeleverd met licentie) |
| Link naar de privacyverklaring van de moskee | Categorieën van activiteiten en lezingen |
| Backendadres voor releases (`backend.apiBaseUrl`) | Firebase-project van het platform |
| Firebase-app (Android en iOS) en de sleutels om de app te tekenen, in een GitHub Environment | Status-, fout- en neutrale kleuren, maten, afstanden |

Gegevens die de moskee zelf bijhoudt, horen niet in `brands/`: adres, contactgegevens, beschrijving, website en
tijdzone staan in de backend en worden in het portaal beheerd (Moskeegegevens). Zo kan de moskee ze wijzigen zonder
nieuwe app-versie.

## Hoe het werkt

- `brands/<slug>/brand.json` is de configuratie, gecontroleerd tegen [`brands/brand.schema.json`](brands/brand.schema.json).
  De controle (`tools/brand/lib.mjs`) heeft geen dependencies en meldt alle fouten in één keer, in het Nederlands.
- `brands/<slug>/mark.svg` is het beeldmerk: viewBox `0 0 96 96`, alleen de kleuren `primary.800` en `accent.400`
  (dan kloppen ook de lichte, donkere en monochrome varianten), geen tekst, scripts, afbeeldingen of links.
- `node tools/brand/brand.mjs apply <slug>` schrijft alles wat van het merk afhangt:

  | Bestand | Wat |
  |---|---|
  | `frontend/app/lib/core/brand/brand.g.dart` | Slug, naam en privacylink voor de app |
  | `frontend/app/android/brand.properties` | Application ID en naam onder het icoon (gelezen door Gradle) |
  | `frontend/app/ios/Flutter/Brand.xcconfig` | Bundle ID en naam onder het icoon (gelezen door Xcode) |
  | `frontend/app/lib/core/design/tokens.g.dart`, `frontend/webportaal/src/styles/tokens.generated.css` | Design tokens met de merkkleuren |
  | `design/branding/svg/`, `png/`, app- en portaalassets, Android- en iOS-iconen | Logo's en iconen uit het beeldmerk |
  | `frontend/webportaal/src/app/core/brand/brand.generated.ts`, `src/index.html` | Naam in het portaal |

- De repository bevat altijd het **standaardmerk** (`brands/index.json`, nu `noeroel-islam`). CI controleert dat met
  `brand.mjs check`. Een ander merk wordt alleen tijdens een build toegepast en niet gecommit.
- CI bouwt en test elke map in `brands/`: Flutter-analyse en -tests, een getekende Android-release (AAB, met een
  wegwerpsleutel) en een iOS-simulatorbuild, en controleert dat ID's en naam in de build kloppen.

## Een nieuwe moskee toevoegen

Voorbeeld: Moskee Al-Fath in Utrecht. Stap 1 t/m 4 in de repository, 5 en 6 eenmalig in externe diensten.

1. **Merk aanmaken**

   ```bash
   node tools/brand/brand.mjs new al-fath --name="Al-Fath" --primary=#1F3A5F --accent=#C8794A
   ```

   Dit maakt `brands/al-fath/brand.json` met kleurschalen rond de opgegeven kleuren en een tijdelijk beeldmerk, en
   controleert meteen het contrast (WCAG 2.2 AA). De ID's worden `nl.mijnummah.alfath`; een andere ID kan met
   `--application-id=` en `--bundle-id=`. **Een ID ligt vast zodra de app in een store staat.**

2. **Aanvullen**
   - `mark.svg`: vervang het tijdelijke beeldmerk door dat van de moskee (eisen hierboven). Laat het ontwerp
     controleren door iemand van de moskee.
   - `launcherName` als de naam langer is dan ongeveer 12 tekens (de naam onder het icoon wordt anders afgekapt).
   - `links.privacyPolicyUrl`: de privacyverklaring van de moskee (zie PRIVACY.md). Zonder deze link toont de app
     er geen; de stores vragen er wel om.
   - `backend.apiBaseUrl`: het https-adres van de API. De release-workflow weigert zonder.

3. **Controleren en bekijken**

   ```bash
   pip install -r design/requirements.txt    # eenmalig, voor de logo's en iconen
   node tools/brand/brand.mjs validate al-fath
   node tools/brand/brand.mjs apply al-fath   # daarna: flutter run in frontend/app
   node tools/brand/brand.mjs apply           # terug naar het standaardmerk vóór het committen
   ```

   Faalt het contrast, pas dan de tinten in `brand.json` aan; de melding zegt welk paar te weinig contrast heeft.

4. **Pull request** met alleen `brands/al-fath/`. CI bouwt en test de nieuwe app naast de bestaande.

5. **Moskee in de backend**: een platformbeheerder kiest in het portaal *Moskee toevoegen* (menu rechtsboven, of
   op de pagina *Alle moskeeën*, waar je moskeeën ook opent en (de)activeert) en
   gebruikt **dezelfde slug** als de map. Vul daarna de moskeegegevens in en voeg een beheerder toe bij Beheerders
   (een nieuw account maak je bij Accounts). Een nieuwe moskee geeft niemand toegang tot iets anders: dat is getest
   in `TenantIsolationIT`.

6. **Firebase en ondertekening** (pas nodig voor een release):
   - In het Firebase-project van het platform een Android-app (`android.applicationId`) en een iOS-app
     (`ios.bundleId`) toevoegen. De backend gebruikt één serviceaccount voor alle apps.
   - In GitHub (Settings > Environments) een Environment met de naam van de slug, met:

     | Soort | Naam | Inhoud |
     |---|---|---|
     | Variable | `FIREBASE_PROJECT_ID`, `FIREBASE_MESSAGING_SENDER_ID` | Uit de Firebase-console |
     | Secret | `FIREBASE_ANDROID_API_KEY`, `FIREBASE_ANDROID_APP_ID`, `FIREBASE_IOS_API_KEY`, `FIREBASE_IOS_APP_ID` | Uit de Firebase-console, bij de app van deze moskee |
     | Secret | `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | De uploadsleutel van deze app (hieronder) |

     Zet bij de Environment *Required reviewers*, dan start een release pas na goedkeuring.

## Ondertekening

Sleutels en certificaten staan **nooit** in de repository of in een commit, ook niet versleuteld.

- **Android**: per app één uploadsleutel, gemaakt door wie de Play Console-account van de moskee beheert:

  ```bash
  keytool -genkeypair -keystore al-fath-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 9125
  base64 -i al-fath-upload.jks | pbcopy        # macOS; plak als ANDROID_KEYSTORE_BASE64
  ```

  Bewaar het `.jks`-bestand en de wachtwoorden in de wachtwoordkluis van de organisatie. Gebruik in Play Console
  *Play App Signing*: Google bewaart dan de echte app-sleutel, en een verloren uploadsleutel is te vervangen.
  Gradle leest de sleutel alleen uit de omgevingsvariabelen `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS` en `ANDROID_KEY_PASSWORD`; zonder die variabelen tekent een release-build met de debugsleutel en
  is hij niet te uploaden.
- **iOS**: ondertekenen vraagt een Apple Developer-account met distributiecertificaat en provisioningprofiel per
  bundle ID. Dat account is er nog niet; de release-workflow bouwt iOS daarom ongetekend.

## Release

De workflow [Release-app](.github/workflows/release-app.yml) start alleen met de hand (Actions > Release-app >
Run workflow, met slug en platform). Hij controleert en past het merk toe, draait de tests, bouwt met de Firebase-
instellingen en het backendadres van het merk, tekent (Android) en levert het bestand als artifact. **Hij uploadt
niets naar Play Console of App Store Connect.** Automatisch publiceren komt pas als de accounts, rechten,
goedkeuringen en releasecontroles er zijn (zie hieronder).

Releasecontrole, met de hand, vóór het uploaden:

- [ ] Versienummer in `frontend/app/pubspec.yaml` verhoogd; het buildnummer is het runnummer van de workflow.
- [ ] De build is getest op een echt toestel tegen de productie-API: inloggen, volgen, een pushmelding ontvangen.
- [ ] Naam, icoon en kleuren gecontroleerd door de moskee.
- [ ] Privacyverklaring staat online op `links.privacyPolicyUrl`; storevermelding en screenshots kloppen.
- [ ] Release-opmerkingen geschreven.

Daarna uploadt iemand met rechten in de store-account het bestand (eerst naar een interne test- of TestFlight-groep).

## Gescheiden gegevens per moskee

Eén backend voor alle apps betekent dat de scheiding in de backend zit, niet in de app:

- Elke beheeractie controleert eerst de rol van de gebruiker voor die moskee en zoekt objecten op met
  `(id, mosque_id)`. `TenantIsolationIT` probeert als beheerder van moskee A elke beheerroute van moskee B
  (`403`), B's ID's in A's routes (`404`), een nieuwe moskee aan te maken of te (de)activeren, en controleert dat
  een nieuwe moskee niemand toegang geeft.
- Een pushtoken hoort bij de app waaruit het is aangemeld (`push_device.mosque_id`, V13). Volgt iemand twee
  moskeeën met elk een eigen app, dan komt een melding van B alleen in de app van B (`PushFlowIT`).
- Publieke routes bevatten de slug van de moskee; de app vraagt alleen die van haar eigen merk op.

## Uitbreiden

- **Ander lettertype per moskee**: kan als uitbreiding (een `typography`-blok in het schema dat een meegeleverd font
  kiest), maar alleen met fonts waarvan de licentie meeleveren in een app toestaat. Nu bewust centraal.
- **Portaal per moskee of als platform**: het portaal is één installatie voor alle moskeeën en toont nu het
  standaardmerk. Bij meer moskeeën ligt een eigen "Mijn Ummah"-merk voor het portaal voor de hand: een map
  `brands/mijn-ummah/` en het portaal bouwen met dat merk (D-050).
- **Eigen categorieën per moskee**: categorieën zijn nu vaste waarden in de backend; per moskee instelbaar maken
  vraagt een migratie en beheer in het portaal.

## Commando's

| Commando | Wat |
|---|---|
| `node tools/brand/brand.mjs list` | Alle merken, met het standaardmerk |
| `node tools/brand/brand.mjs validate [slug...]` | Schema, beeldmerk, dubbele ID's en contrast |
| `node tools/brand/brand.mjs new <slug> --name= --primary= --accent=` | Nieuw merk |
| `node tools/brand/brand.mjs apply [slug] [--skip-assets]` | Merk toepassen (zonder slug: het standaardmerk) |
| `node tools/brand/brand.mjs check` | Faalt als de repository niet het standaardmerk weergeeft |
| `node tools/brand/brand.mjs show <slug> <veld>` | Eén waarde, bijv. `android.applicationId` |
