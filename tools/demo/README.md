# Testmoskeeën klaarzetten

`seed-demo.mjs` zet testmoskeeën met beheerders, logo, omslagfoto en content klaar. Het script gebruikt alleen de
API die ook de portalen gebruiken: de platformbeheerder registreert de moskee, daarna richt de eigen beheerder (of
redacteur) haar in. Er komt niets in de code of de database dat niet ook via het portaal kan.

Vereist: Node 20 of nieuwer, en een draaiende backend.

```bash
API_BASE_URL=http://localhost:8082 \
PLATFORM_ADMIN_EMAIL=admin@mijnummah.local PLATFORM_ADMIN_PASSWORD=lokaal-wachtwoord-123 \
DEMO_PASSWORD=kies-een-wachtwoord \
node tools/demo/seed-demo.mjs
```

- `DEMO_PASSWORD` wordt het wachtwoord van de nieuwe beheer- en redactieaccounts. Wachtwoorden komen alleen uit de
  omgeving en worden niet getoond.
- De moskeeën staan in [demo-mosques.json](demo-mosques.json): twee verzonnen moskeeën in Utrecht en Rotterdam met
  adressen op `.test`. Een eigen bestand kan als argument: `node tools/demo/seed-demo.mjs mijn-moskeeen.json`.
- Een moskee waarvan de slug al bestaat, wordt overgeslagen. Het script wijzigt of verwijdert nooit bestaande
  gegevens en kan dus opnieuw worden gedraaid.
- De video's verwijzen naar verzonnen YouTube-ID's; de app toont dan dat de video niet beschikbaar is. Vervang ze in
  het portaal door echte lezingen.
- Alleen voor lokale omgevingen en testomgevingen, niet voor productie.
