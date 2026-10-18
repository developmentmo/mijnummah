# API

Basis-URL `/api/v1`. JSON, tijden als ISO-8601 in UTC (`2026-11-06T17:30:00Z`). De volledige, gegenereerde
specificatie staat op `/v3/api-docs`; dit document beschrijft de regels erachter.

## Authenticatie

1. `POST /auth/login` met `{email, password}` geeft een access token (JWT, 15 minuten) en een refresh token
   (opaak, 30 dagen).
2. Stuur `Authorization: Bearer <accessToken>` mee.
3. `POST /auth/refresh` met `{refreshToken}` geeft een nieuw paar; het oude refresh token vervalt direct.
   Wordt een al gebruikt refresh token nog eens aangeboden, dan wordt de hele sessie ingetrokken (diefstaldetectie).
4. `POST /auth/logout` met `{refreshToken}` beëindigt de sessie.

Een meegestuurd token wordt ook op publieke endpoints gecontroleerd: een verlopen token geeft daar `401`. Ververs het
token of stuur de header niet mee.

Rollen en status worden bij elk verzoek uit de database gelezen: een geblokkeerd account is direct uitgesloten.
Na 10 mislukte pogingen voor hetzelfde e-mailadres volgt `429` met `Retry-After`. Accounts worden in deze fase door
een platformbeheerder aangemaakt; zelf registreren komt in fase 5.

## Autorisatie

| Wie | Mag |
|---|---|
| Iedereen | `GET /public/**`: actieve moskeeën en hun gepubliceerde content met zichtbaarheid `PUBLIC` |
| Ingelogde gebruiker | Daarnaast content met zichtbaarheid `REGISTERED_USERS`, aanmelden voor activiteiten, moskeeën volgen |
| `EDITOR` van moskee X | Activiteiten, media en aankondigingen van X beheren |
| `ADMIN` van moskee X | Alles van EDITOR, plus gegevens van X wijzigen, aanmeldingen inzien, beheerders beheren |
| `PLATFORM_ADMIN` | Alles, voor alle moskeeën; moskeeën en accounts aanmaken, moskeeën (de)activeren, accounts blokkeren |

Beheerendpoints hebben de vorm `/admin/mosques/{mosqueId}/...`. Bij elk verzoek controleert de server eerst of de
gebruiker rechten heeft voor precies die moskee (`403 MOSQUE_ACCESS_DENIED` als dat niet zo is) en zoekt daarna
het object op met de combinatie (id, moskee). Een ID van een andere moskee geeft daardoor `404`, ook voor een
platformbeheerder die via de verkeerde moskee-route werkt. De client bepaalt dus nooit zelf tot welke moskee iets
hoort.

## Endpoints

### Publiek (geen account nodig)

| Methode | Pad | |
|---|---|---|
| GET | `/public/mosques` | Actieve moskeeën |
| GET | `/public/mosques/{slug}` | Profiel: naam, beschrijving, adres, contact, tijdzone |
| GET | `/public/mosques/{slug}/events` | Agenda, oplopend. Filters: `category`, `from` (standaard nu: alleen wat nog niet voorbij is), `to`, `page`, `size` |
| GET | `/public/mosques/{slug}/events/{id}` | Activiteit, met plaatsen vrij en (ingelogd) `registeredByMe` |
| GET | `/public/mosques/{slug}/media` | Mediabibliotheek, nieuwste eerst. Filters: `q` (titel en spreker), `category` |
| GET | `/public/mosques/{slug}/media/{id}` | Media-item met `video.embedUrl` (youtube-nocookie.com) en `video.watchUrl` |
| GET | `/public/mosques/{slug}/media/{id}/related` | Maximaal 4 items met dezelfde categorie of spreker |
| GET | `/public/mosques/{slug}/announcements` | Actuele aankondigingen; uitgelicht eerst, verlopen niet |
| GET | `/public/mosques/{slug}/announcements/{id}` | Aankondiging |

Concepten zijn nooit publiek. Geannuleerde activiteiten blijven zichtbaar met `status: CANCELLED`.

### Ingelogde gebruiker

| Methode | Pad | |
|---|---|---|
| GET | `/auth/me` | Eigen account |
| POST | `/events/{eventId}/registrations` | Aanmelden (`201`) |
| DELETE | `/events/{eventId}/registrations/me` | Eigen aanmelding annuleren (`204`) |
| GET | `/me/event-registrations` | Eigen aanmeldingen |
| GET | `/me/follows` | Gevolgde moskeeën met meldingsvoorkeuren |
| PUT / DELETE | `/me/follows/{mosqueId}` | Volgen / ontvolgen (beide idempotent) |
| GET | `/me/follows/{mosqueId}` | Eén gevolgde moskee |
| PUT | `/me/follows/{mosqueId}/notifications` | `{announcements?, events?, media?}`; weggelaten onderwerpen blijven gelijk |
| PUT | `/me/devices` | Pushtoken van dit toestel `{token, platform, mosqueSlug}` (`204`, idempotent); `mosqueSlug` is de moskee van de app |
| POST | `/me/devices/remove` | Pushtoken afmelden `{token}` (`204`, idempotent; body in plaats van URL zodat tokens niet in logs komen) |

Aanmelden kan alleen voor een gepubliceerde, zichtbare activiteit met aanmelding aan, die nog niet begonnen is en
niet vol zit. De capaciteit wordt bewaakt met een rijvergrendeling op de activiteit, ook bij gelijktijdige
aanmeldingen. Meldingsvoorkeuren worden in deze fase alleen opgeslagen; pushmeldingen volgen in fase 5.

### Beheer (per moskee)

Onder `/admin/mosques/{mosqueId}`:

| Pad | Acties |
|---|---|
| (de moskee zelf) | GET (EDITOR), PUT gegevens (ADMIN) |
| `/administrators` | GET, POST `{email, role}`, PUT `/{id}` `{role}`, DELETE `/{id}` (ADMIN); de laatste ADMIN blijft altijd |
| `/events` | GET (filters `status`, `from`), POST, GET/PUT/DELETE `/{id}`, POST `/{id}/publish`, `/unpublish`, `/cancel` |
| `/events/{id}/registrations` | GET: wie zich heeft aangemeld (ADMIN; bevat persoonsgegevens) |
| `/media` | GET (filters `status`, `q`), POST, GET/PUT/DELETE `/{id}`, POST `/{id}/publish`, `/unpublish` |
| `/announcements` | GET (filter `status`), POST, GET/PUT/DELETE `/{id}`, POST `/{id}/publish`, `/unpublish` |

Nieuwe content is altijd eerst een concept. Regels: een activiteit met aanmeldingen kan niet terug naar concept
(annuleer haar), alleen concepten zonder aanmeldingen kunnen worden verwijderd, annuleren is definitief, de
capaciteit kan niet onder het aantal aanmeldingen. Een aankondiging publiceren verstuurt geen pushmelding.
Een video wordt opgegeven als YouTube-link of video-ID; de backend slaat alleen de verwijzing op.

Een pushmelding (POST `/{events|media|announcements}/{id}/push`, ADMIN) gaat naar de actieve volgers van deze
moskee met het onderwerp aan, en alleen naar hun toestellen in de app van deze moskee. Volgt iemand ook een andere
moskee met diens eigen app, dan komt de melding daar niet (white-label, D-049).

### Platform

| Methode | Pad | |
|---|---|---|
| GET | `/admin/mosques` | De moskeeën die ik beheer (platformbeheerder: alle), met mijn rol |
| POST | `/admin/mosques` | Moskee aanmaken `{slug, details}` (platformbeheerder) |
| PUT | `/admin/mosques/{id}/active` | (De)activeren; inactief verbergt de moskee en al haar content publiek |
| GET, POST | `/admin/users` | Accounts tonen en aanmaken (platformbeheerder) |
| POST | `/admin/users/{id}/block`, `/unblock` | Blokkeren beëindigt alle sessies direct |

## Paginering

Lijsten geven `{items, page, size, totalItems, totalPages}`. `page` begint bij 0, `size` is standaard 20 en
maximaal 100.

## Fouten

Elke fout heeft dezelfde vorm; clients reageren op `code`, niet op `message`:

```json
{"timestamp": "2026-10-11T10:00:00Z", "status": 409, "code": "EVENT_FULL",
 "message": "This event is full.", "path": "/api/v1/events/…/registrations"}
```

Bij `VALIDATION_FAILED` staat er ook `fieldErrors: [{field, message}]` in. Veelgebruikte codes:

| Status | Codes |
|---|---|
| 400 | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `INVALID_PARAMETER`, `INVALID_PASSWORD`, `INVALID_EVENT`, `INVALID_MOSQUE`, `INVALID_ANNOUNCEMENT`, `INVALID_VIDEO_URL`, `INVALID_RELATED_EVENT` |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS`, `INVALID_REFRESH_TOKEN` |
| 403 | `FORBIDDEN`, `MOSQUE_ACCESS_DENIED`, `ACCOUNT_BLOCKED` |
| 404 | `MOSQUE_NOT_FOUND`, `EVENT_NOT_FOUND`, `MEDIA_NOT_FOUND`, `ANNOUNCEMENT_NOT_FOUND`, `REGISTRATION_NOT_FOUND`, `NOT_FOLLOWING`, `USER_NOT_FOUND`, `ADMINISTRATOR_NOT_FOUND` |
| 409 | `ALREADY_REGISTERED`, `EVENT_FULL`, `EVENT_HAS_REGISTRATIONS`, `VIDEO_ALREADY_ADDED`, `SLUG_ALREADY_TAKEN`, `EMAIL_ALREADY_REGISTERED`, `ADMINISTRATOR_ALREADY_EXISTS`, `CONCURRENT_MODIFICATION` |
| 422 | `EVENT_CANCELLED`, `EVENT_NOT_PUBLISHED`, `EVENT_NOT_DRAFT`, `REGISTRATION_NOT_ENABLED`, `REGISTRATION_CLOSED`, `CAPACITY_BELOW_REGISTRATIONS`, `LAST_ADMIN`, `CANNOT_CHANGE_OWN_ACCOUNT` |
| 429 | `TOO_MANY_LOGIN_ATTEMPTS`, `RATE_LIMITED` |
