# Huisstijl Noeroel Islam

Deze richtlijnen beschrijven de huisstijl van de Noeroel Islam-app en het beheerportaal. De merkkleuren, naam en het
beeldmerk staan in [`brands/noeroel-islam/`](brands/noeroel-islam/brand.json); al het andere (typografie, maten,
status- en neutrale kleuren) in [`design/tokens/tokens.json`](design/tokens/tokens.json). Pas waarden daar aan en
draai `node tools/brand/brand.mjs apply`; nooit rechtstreeks in de app of het portaal. Andere moskeeën hebben een
eigen merk met dezelfde regels voor gebruik en contrast, zie [WHITE_LABEL.md](WHITE_LABEL.md).

**Merkbelofte:** een toegankelijke digitale verbinding tussen de moskee en de gemeenschap.
**Uitstraling:** modern, warm, betrouwbaar, spiritueel en toegankelijk. Rustig in plaats van druk: geen zware
patronen, goud alleen als accent.

---

## 1. Logo

### Concept

Het beeldmerk is een minimalistische **mihrab-boog** die onderaan open is, als een uitnodigende doorgang.
In de boog staat een **vlam**: licht ("noer") en kennis. Een korte lijn onder de vlam verbindt de twee zijden
van de boog en staat voor verbinding met de gemeenschap. De woordmark **Noeroel Islam** is gezet in Fraunces
SemiBold en omgezet naar paden.

### Varianten

| Bestand (`design/branding/svg/`) | Gebruik |
|---|---|
| `logo-horizontal-light.svg` | Standaardlogo op lichte achtergronden (headers, documenten) |
| `logo-horizontal-dark.svg` | Op donkergroen of andere donkere achtergronden |
| `logo-horizontal-mono-black.svg` / `-mono-white.svg` | Eén kleur: drukwerk, gravure, stempels, foto-achtergronden |
| `logo-stacked-light.svg` / `-dark.svg` | Gecentreerd: welkomst- en startschermen, posters |
| `logo-compact-light.svg` / `-dark.svg` / `-mono-*.svg` | Alleen beeldmerk: kleine oppervlakken, avatar, watermerk |
| `app-icon.svg` | App-icoon (vol vlak; het platform rondt de hoeken af) |
| `app-icon-foreground.svg` / `-background.svg` / `-monochrome.svg` | Android adaptive en themed icon |
| `favicon.svg` | Browsertabblad van het beheerportaal |

Rasterexports staan in `design/branding/png/` (App Store 1024×1024 zonder transparantie, Play Store 512×512).
Alle bestanden worden gegenereerd door `design/branding/build_branding.py`.

### Regels

- **Vrije ruimte:** houd rondom het logo minimaal de breedte van de vlam vrij.
- **Minimale maat:** horizontaal logo minimaal 24 px hoog op schermen; compact beeldmerk minimaal 16 px.
  Onder 32 px het compacte beeldmerk gebruiken.
- **Kleuren:** alleen de aangeleverde varianten. Op foto's de monochrome witte variant met voldoende contrast.
- **Niet doen:** het logo uitrekken of draaien, de vlam een andere kleur geven, schaduwen of verlopen toevoegen,
  de woordmark in een ander font zetten, het logo op een drukke achtergrond plaatsen of omkaderen.

---

## 2. Kleur

### Merkkleuren

| Naam | Hex | Token | Rol |
|---|---|---|---|
| Donkergroen | `#123C32` | `palette.primary.800` | Primaire merkkleur, knoppen, koppen op licht |
| Jadegroen | `#2F7965` | `palette.primary.500` | Secundaire acties, focus, accenten |
| Warm ivoor | `#F7F3E9` | `palette.neutral.50` | Achtergrond van app en portaal |
| Gedempt goud | `#C6A45A` | `palette.accent.400` | Accent: vlam, dunne lijnen, highlights op donkergroen |
| Donkergrijs | `#242A27` | `palette.neutral.900` | Tekst |
| Wit | `#FFFFFF` | `palette.neutral.0` | Kaarten en oppervlakken |

Elke kleur heeft een tintenreeks (50–950) voor achtergronden, randen en hover-, pressed- en focustoestanden.
Daarnaast zijn er statuskleuren: blad (succes), amber (waarschuwing), rood (fout) en blauw (informatie).

### Semantische kleuren

Gebruik in de code altijd de **semantische** namen, nooit een paletkleur. Zo werken licht en donker vanzelf.

| Token | Licht | Donker | Gebruik |
|---|---|---|---|
| `background` | ivoor | `#0C1D18` | Paginaachtergrond |
| `surface` | wit | `#12271F` | Kaarten, invoervelden, navigatie |
| `onSurface` | donkergrijs | `#F2EEE4` | Tekst |
| `onSurfaceMuted` | `neutral.600` | `#B4BEB8` | Toelichting, metadata |
| `primary` / `onPrimary` | donkergroen / wit | `primary.200` / `primary.900` | Primaire knoppen |
| `secondary` | jadegroen | `primary.300` | Secundaire acties |
| `accent` | goud | goud | Decoratief accent, nooit voor tekst op licht |
| `accentText` | `accent.700` | `accent.300` | Gouden tekst (labels) die wel leesbaar is |
| `focus` | jadegroen | `accent.300` | Focusring van toetsenbordnavigatie |
| `border` / `borderStrong` | `neutral.200` / `neutral.500` | `#2B463D` / `#6F8C81` | Scheidingslijnen / randen van invoervelden |
| `success`, `warning`, `error`, `info` (+ `…Container`, `on…Container`) | | | Meldingen en statussen |

### Goud: alleen als accent

Goud haalt op lichte achtergronden geen leesbaar contrast (2,14:1 op ivoor). Daarom:

- Goud **wel**: in het logo, als dunne lijn of icoonaccent, en als tekst of icoon op donkergroen (5,16:1).
- Goud **niet**: als tekst, icoon of knopkleur op ivoor of wit. Gebruik daar `accentText`.
- Nooit grote gouden vlakken; het merk blijft groen en warm, niet glimmend.

### Contrast

Alle tekst- en achtergrondcombinaties uit de tokens worden automatisch gecontroleerd op **WCAG 2.2 AA**
(`node design/tokens/contrast.mjs`, ook in CI). Normale tekst minimaal 4,5:1, grote tekst en niet-tekstuele
elementen (focusring, randen van invoervelden) minimaal 3:1. Enkele waarden:

| Combinatie | Licht | Donker |
|---|---|---|
| Tekst op achtergrond | 13,20:1 | 15,05:1 |
| Gedempte tekst op achtergrond | 5,91:1 | 9,13:1 |
| Tekst op primaire knop | 12,22:1 | 9,32:1 |
| Tekst op secundaire knop | 5,19:1 | |
| Gouden tekst (`accentText`) op achtergrond | 6,27:1 | 9,62:1 |
| Focusring op achtergrond | 4,69:1 | |

---

## 3. Typografie

| Rol | Font | Waarom |
|---|---|---|
| Koppen | **Fraunces** (SemiBold, zachte vorm: SOFT 50, WONK 0) | Warm en eigentijds, met karakter zonder ouderwets te worden |
| Tekst en interface | **Inter** (Regular, Medium, SemiBold, Bold) | Zeer leesbaar op kleine schermen en voor oudere gebruikers |
| Arabische tekst | **Noto Naskh Arabic** | Klassiek, goed leesbaar Naskh-schrift; alleen voor Arabische citaten |

Alle drie zijn open source onder de **SIL Open Font License 1.1** (licenties in `design/fonts/`, en naast de
fontbestanden in de app en het portaal). De fonts worden meegeleverd, er worden geen fonts van externe servers
geladen.

### Schaal

| Stijl | Font | Grootte / regelhoogte | Gebruik |
|---|---|---|---|
| displayLarge | Fraunces 600 | 40 / 48 | Welkomsttitel |
| displayMedium | Fraunces 600 | 32 / 40 | Paginatitel |
| headlineLarge | Fraunces 600 | 28 / 36 | Grote sectiekop |
| headlineMedium | Fraunces 600 | 24 / 32 | Sectiekop |
| headlineSmall | Fraunces 600 | 20 / 28 | Kleine kop, dialoogtitel |
| titleLarge | Inter 600 | 20 / 28 | Titel van een card |
| titleMedium | Inter 600 | 17 / 24 | Lijstitem, subtitel |
| titleSmall | Inter 600 | 15 / 22 | Kleine titel |
| bodyLarge | Inter 400 | 17 / 26 | Standaard lopende tekst |
| bodyMedium | Inter 400 | 15 / 22 | Compacte tekst |
| bodySmall | Inter 400 | 13 / 18 | Metadata, toelichting |
| labelLarge | Inter 600 | 15 / 20 | Knoppen |
| labelMedium | Inter 500 | 13 / 18 | Chips, navigatielabels |
| labelSmall | Inter 500 | 12 / 16 | Kleine labels (categorie boven een titel) |

Regels: tekst schaalt mee met de lettergrootte van het toestel of de browser (getest tot 200%). Gebruik Fraunces
alleen voor koppen, nooit voor lopende tekst of knoppen. Geen tekst kleiner dan 12 px.

---

## 4. Ruimte, vorm en diepte

- **Afstand:** stappen van 4: `space.1` = 4, `2` = 8, `3` = 12, `4` = 16, `5` = 20, `6` = 24, `8` = 32, `10` = 40,
  `12` = 48, `16` = 64. Standaard paginamarge 16 (mobiel) tot 24 (desktop).
- **Afronding:** `xs` 4, `sm` 8, `md` 12 (knoppen, invoervelden, meldingen), `lg` 16 (cards), `xl` 24 (dialogen,
  bottom sheets), `full` (chips, navigatie-indicator).
- **Schaduw:** spaarzaam. Cards gebruiken een rand (`border`) in plaats van schaduw; `shadow.md` en `shadow.lg` alleen
  voor zwevende elementen (menu's, dialogen).
- **Beweging:** 120 / 200 / 320 ms met een rustige easing. Bij "minder beweging" staan animaties stil.
- **Iconen:** Material Icons (outlined; gevuld voor de actieve tab), maten 16 / 20 / 24 / 32.

---

## 5. Componentregels

| Component | Regel |
|---|---|
| Knoppen | Hoogte 44 (minimaal aanraakdoel 48 breed). Eén primaire knop per scherm of formulier. Tekst is een werkwoord ("Aanmelden", "Publiceren"). Uitgeschakelde knoppen alleen met uitleg waarom. |
| Invoervelden | Hoogte 48, altijd een zichtbaar label (geen placeholder als label). Foutmelding onder het veld, in gewone taal, met een voorbeeld van wat wel klopt. |
| Cards | Wit oppervlak, rand, afronding 16, padding 16. Afbeelding bovenaan, daarboven nooit tekst over een foto zonder contrastlaag. |
| Navigatie (app) | Vijf tabs: Home, Agenda, Lezingen, Ontdekken, Profiel. Icoon plus label, actieve tab met groene indicator. |
| Navigatie (portaal) | Zijbalk van 264 px op desktop, inklapbaar op kleine schermen. |
| Meldingen | Binnen de pagina met icoon, kleur én tekstlabel (betekenis nooit alleen via kleur). Fouten bieden een herstelactie. |
| Dialogen | Alleen voor bevestiging van risicovolle acties. Afronding 24, maximaal 560 breed. De veilige keuze staat links of is de standaard. |
| Laden | Skeletons in de vorm van de content; geen draaiende spinners voor hele pagina's. |
| Leeg / fout | Icoon, korte uitleg en een vervolgstap ("Opnieuw proberen", "Evenement aanmaken"). |

De componenten zijn te zien in de interne designpreview: in de Flutter-app (start nu direct in de preview) en in het
portaal op `/`.

---

## 6. Beeldtaal

- Gebruik bij voorkeur eigen foto's van Noeroel Islam, met toestemming van de mensen die herkenbaar in beeld zijn.
- Warm, natuurlijk licht; rustige composities; geen zware filters.
- Geen stockfoto's van personen die de indruk wekken dat zij bij Noeroel Islam betrokken zijn.
- Zolang er geen eigen beeld is: neutrale plaatshouders in de huisstijl (icoon op `primaryContainer`) met het label
  **Voorbeeldafbeelding**. Voorbeeldcontent is altijd als voorbeeld herkenbaar.
- Respecteer auteurs- en portretrecht; leg bij lezingen en evenementen vast wie toestemming gaf voor publicatie.

---

## 7. Toegankelijkheid

- WCAG 2.2 AA als minimum; contrast wordt automatisch gecontroleerd.
- Tekst schaalbaar tot 200% zonder dat content wegvalt (getest in de Flutter-widgettests).
- Aanraakdoelen minimaal 48×48.
- Zichtbare focusring (2 px, jadegroen in licht, goud in donker) bij toetsenbordnavigatie.
- Logo's en iconen met betekenis hebben een toegankelijke naam; decoratieve elementen worden overgeslagen.
- Meldingen hebben naast kleur een icoon en een voorleesbaar label; foutmeldingen worden direct voorgelezen.
- Animaties respecteren de systeeminstelling voor minder beweging.
- Taal is Nederlands (`lang="nl"`); Arabische tekst krijgt `lang="ar"` en leesrichting van rechts naar links.

---

## 8. Werken met de huisstijl

| Taak | Commando |
|---|---|
| Tokens genereren na een wijziging | `node design/tokens/build.mjs` |
| Controleren of de gegenereerde bestanden actueel zijn | `node design/tokens/build.mjs --check` |
| Contrast controleren | `node design/tokens/contrast.mjs --report` |
| Zoeken naar losse kleuren of fonts | `node design/tokens/check-hardcoded.mjs` |
| Fonts opnieuw genereren | `python3 design/fonts/build_fonts.py` |
| Logo's en app-iconen opnieuw genereren | `python3 design/branding/build_branding.py` |
| Logo's en iconen controleren | `python3 design/branding/check_assets.py` |

De Python-scripts vereisen `pip install -r design/requirements.txt`. De controles draaien ook in CI.
