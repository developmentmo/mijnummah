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
| `PLATFORM_ADMIN` | Alles, voor alle moskeeën; moskeeën en accounts aanmaken, status van moskeeën wijzigen, accounts blokkeren, het hele auditlog |

Beheerendpoints hebben de vorm `/admin/mosques/{mosqueId}/...`. Bij elk verzoek controleert de server eerst of de
gebruiker rechten heeft voor precies die moskee (`403 MOSQUE_ACCESS_DENIED` als dat niet zo is) en zoekt daarna
het object op met de combinatie (id, moskee). Een geblokkeerde moskee (`BLOCKED`) kan alleen een platformbeheerder
nog beheren; haar eigen beheerders en redacteuren krijgen `403`. Een ID van een andere moskee geeft daardoor `404`, ook voor een
platformbeheerder die via de verkeerde moskee-route werkt. De client bepaalt dus nooit zelf tot welke moskee iets
hoort.

## Endpoints

### Publiek (geen account nodig)

| Methode | Pad | |
|---|---|---|
| GET | `/public/mosques` | Actieve moskeeën, alfabetisch, gepagineerd. Filters: `q` (naam), `city` (plaats, hoofdletterongevoelig), `page`, `size` |
| GET | `/public/mosques/cities` | Plaatsen met een actieve moskee, alfabetisch |
| GET | `/public/mosques/{slug}` | Profiel: naam, beschrijving, adres, contact, tijdzone, `logoUrl` en `coverUrl` (pad of `null`) |
| GET | `/public/mosques/{slug}/events` | Agenda, oplopend. Filters: `category`, `from` (standaard nu: alleen wat nog niet voorbij is), `to`, `page`, `size` |
| GET | `/public/mosques/{slug}/events/{id}` | Activiteit, met plaatsen vrij en (ingelogd) `registeredByMe` |
| GET | `/public/mosques/{slug}/media` | Mediabibliotheek, nieuwste eerst. Filters: `q` (titel en spreker), `category` |
| GET | `/public/mosques/{slug}/media/{id}` | Media-item met `video.embedUrl` (youtube-nocookie.com) en `video.watchUrl` |
| GET | `/public/mosques/{slug}/media/{id}/related` | Maximaal 4 items met dezelfde categorie of spreker |
| GET | `/public/mosques/{slug}/announcements` | Actuele aankondigingen; uitgelicht eerst, verlopen niet |
| GET | `/public/mosques/{slug}/announcements/{id}` | Aankondiging |
| GET | `/public/events` | Agenda van alle actieve moskeeën. Filters als bij één moskee, plus `mosque` (slug, herhaalbaar, maximaal 100; onbekende of inactieve slugs vallen weg) |
| GET | `/public/events/{id}` | Activiteit op id, met `mosque {id, slug, name, logoUrl}`; `404` als de moskee inactief is of de activiteit niet zichtbaar |
| GET | `/public/media` | Videobibliotheek van alle actieve moskeeën. Filters: `mosque` (herhaalbaar), `q`, `category` |
| GET | `/public/media/{id}`, `/public/media/{id}/related` | Media-item op id, gerelateerde items van dezelfde moskee |
| GET | `/public/announcements` | Nieuws van alle actieve moskeeën, nieuwste eerst. Filter: `mosque` (herhaalbaar) |
| GET | `/public/announcements/{id}` | Aankondiging op id |
| GET | `/public/images/{id}` | Afbeelding (JPEG of PNG); verandert nooit, mag een jaar in de cache |

Concepten zijn nooit publiek. Geannuleerde activiteiten blijven zichtbaar met `status: CANCELLED`.

### Ingelogde gebruiker

| Methode | Pad | |
|---|---|---|
| GET | `/auth/me` | Eigen account |
| POST | `/events/{eventId}/registrations` | Aanmelden (`201`) |
| DELETE | `/events/{eventId}/registrations/me` | Eigen aanmelding annuleren (`204`) |
| GET | `/me/event-registrations` | Eigen aanmeldingen |
| GET | `/me/follows` | Gevolgde moskeeën (slug, naam, logo, `favorite`) met meldingsvoorkeuren; favorieten eerst |
| PUT / DELETE | `/me/follows/{mosqueId}` | Volgen / ontvolgen (beide idempotent) |
| GET | `/me/follows/{mosqueId}` | Eén gevolgde moskee |
| PUT / DELETE | `/me/follows/{mosqueId}/favorite` | Favoriete moskee aan (volgt zo nodig) / uit (blijft gevolgd); geeft de volgstatus |
| PUT | `/me/follows/{mosqueId}/notifications` | `{announcements?, events?, media?}`; weggelaten onderwerpen blijven gelijk |
| PUT | `/me/devices` | Pushtoken van dit toestel `{token, platform}` (`204`, idempotent). Het toestel krijgt de meldingen van alle moskeeën die de gebruiker volgt; een `mosqueSlug` van een oudere app wordt genegeerd |
| GET | `/me/notifications` | Meldingen van gevolgde, actieve moskeeën van de laatste 30 dagen, nieuwste eerst: `{id, mosque, type, contentId, title, body, sentAt}` |
| GET | `/me/favorite-media` | Favoriete video's die nu zichtbaar zijn, laatst bewaard eerst (gepagineerd) |
| GET | `/me/favorite-media/ids` | IDs van alle favoriete video's |
| PUT / DELETE | `/me/favorite-media/{mediaId}` | Video bewaren (alleen een zichtbare video, anders `404`) / verwijderen; `204`, idempotent |
| POST | `/me/devices/remove` | Pushtoken afmelden `{token}` (`204`, idempotent; body in plaats van URL zodat tokens niet in logs komen) |

Aanmelden kan alleen voor een gepubliceerde, zichtbare activiteit met aanmelding aan, die nog niet begonnen is en
niet vol zit. De capaciteit wordt bewaakt met een rijvergrendeling op de activiteit, ook bij gelijktijdige
aanmeldingen. Meldingsvoorkeuren worden in deze fase alleen opgeslagen; pushmeldingen volgen in fase 5.

### Beheer (per moskee)

Onder `/admin/mosques/{mosqueId}`:

| Pad | Acties |
|---|---|
| (de moskee zelf) | GET (EDITOR), PUT gegevens (ADMIN), met `prayerTimesUrl` (http(s)-link naar de gebedstijden, optioneel) |
| `/dashboard` | GET (EDITOR): `{followers, newFollowers, upcomingEvents, draftEvents, publishedVideos, scheduledVideos, draftVideos, publishedAnnouncements, draftAnnouncements, recentPushNotifications}`; "nieuw" en "recent" zijn de laatste 30 dagen |
| `/images` | POST multipart `file` (EDITOR): afbeelding uploaden, JPEG of PNG tot 10 MB, geeft `{id, path, width, height}`; PUT `{logoImageId, coverImageId}` (ADMIN): logo en omslagfoto instellen, `null` verwijdert |
| `/administrators` | GET, POST `{email, role}`, PUT `/{id}` `{role}`, DELETE `/{id}` (ADMIN); de laatste ADMIN blijft altijd |
| `/events` | GET (filters `status`, `from`; `status=ARCHIVED` voor het archief), POST, GET/PUT/DELETE `/{id}`, POST `/{id}/publish`, `/unpublish`, `/cancel`, `/archive`, `/restore` |
| `/events/{id}/registrations` | GET: wie zich heeft aangemeld (ADMIN; bevat persoonsgegevens) |
| `/media` | GET (filters `status`, `q`), POST, GET/PUT/DELETE `/{id}`, POST `/{id}/publish` (optioneel `{publishedAt}`), `/unpublish` |
| `/announcements` | GET (filter `status`), POST, GET/PUT/DELETE `/{id}`, POST `/{id}/publish`, `/unpublish` |
| `/notifications` | GET: verstuurde pushmeldingen met status en aantallen (EDITOR); POST `{title, body, audience}` (ADMIN): eigen bericht, `202` |
| `/notifications/audience` | GET met `contentType` of `audience`: hoeveel volgers en toestellen een melding nu bereikt |

Nieuwe content is altijd eerst een concept. Regels: een activiteit met aanmeldingen kan niet terug naar concept
(annuleer haar), alleen concepten zonder aanmeldingen kunnen worden verwijderd, annuleren is definitief, de
capaciteit kan niet onder het aantal aanmeldingen. Een aankondiging publiceren verstuurt geen pushmelding.
Een video wordt opgegeven als YouTube-link of video-ID; de backend slaat alleen de verwijzing op.

Afbeeldingen bij content: `imageId` (activiteit, aankondiging) of `thumbnailImageId` (video) verwijst naar een
upload van dezelfde moskee (`404 IMAGE_NOT_FOUND` anders). Het antwoord geeft `imageUrl` / `thumbnailUrl` als pad
`/api/v1/public/images/{id}`, of de oudere externe link als er geen upload is.

Archiveren kan bij een gepubliceerde of geannuleerde activiteit (een concept geeft `422 EVENT_NOT_PUBLISHED`). Een
gearchiveerde activiteit verdwijnt uit de app en de publieke API, kan niet worden gewijzigd (`EVENT_ARCHIVED`) en
komt met `/restore` terug in haar vorige status.

Een video publiceren met `publishedAt` in de toekomst plant haar in: ze staat op `PUBLISHED`, maar verschijnt pas
vanaf dat moment in de app; een pushmelding kan pas dan (`CONTENT_NOT_PUBLISHED`). Een datum in het verleden kan
(bijv. de opnamedatum); meer dan 366 dagen vooruit geeft `400 INVALID_PUBLICATION_DATE`. Zonder datum blijft een
eerder gekozen datum staan.

Een pushmelding (POST `/{events|media|announcements}/{id}/push`, ADMIN) gaat naar de actieve volgers van deze
moskee met het onderwerp aan. Een eigen bericht (POST `/notifications`) kiest de doelgroep: `ALL_FOLLOWERS`
(volgers met minstens één onderwerp aan), `ANNOUNCEMENTS`, `EVENTS` of `MEDIA`. (Tot fase D zijn toestellen nog aan de moskee van de app gekoppeld, V13; met één
centrale app wordt dat de gebruiker.)

### Platform

| Methode | Pad | |
|---|---|---|
| GET | `/admin/mosques` | De moskeeën die ik beheer (platformbeheerder: alle), met mijn rol |
| POST | `/admin/mosques` | Moskee registreren `{slug?, details, firstAdministrator?: {email, displayName}, settings?: {registrationsEnabled, pushEnabled}}` (platformbeheerder). Zonder slug maakt de backend een unieke uit de naam (`-2`, `-3` …). Een onbekend e-mailadres krijgt een account met uitnodiging per e-mail; een bestaand account wordt ADMIN; een geblokkeerd account geeft `409 ACCOUNT_BLOCKED`. Alles in één transactie. Antwoord: `{mosque, settings, firstAdministrator: {userId, invited} \| null}` |
| GET, PUT | `/admin/mosques/{id}/settings` | Configuratie `{registrationsEnabled, pushEnabled}`. Lezen: EDITOR van de moskee; wijzigen: platformbeheerder. Staat aanmelden uit, dan geeft aanmelden `422 REGISTRATIONS_DISABLED_FOR_MOSQUE`; staat push uit, dan geeft verzenden `422 PUSH_DISABLED_FOR_MOSQUE` |
| PATCH | `/admin/mosques/{id}/status` | `{status}`: `ACTIVE`, `INACTIVE` (verborgen, beheerders kunnen voorbereiden) of `BLOCKED` (verborgen en alleen de platformbeheerder beheert) |
| GET | `/admin/audit-log` | Beheeracties, nieuwste eerst, met `mosqueName`; zonder `mosqueId` alleen platformbeheerder, met `mosqueId` ook ADMIN van die moskee |
| GET | `/admin/platform/dashboard` | Totalen: moskeeën per status, accounts, nieuwe accounts (30 dagen), volgers, activiteiten, gepubliceerde video's en aankondigingen, pushmeldingen (30 dagen) (platformbeheerder) |
| GET | `/admin/platform/mosques` | Alle moskeeën met aantallen beheerders, volgers, activiteiten, video's en aankondigingen (platformbeheerder) |
| GET | `/admin/users` | Accounts zoeken: `q` (naam of e-mail), `status` (`ACTIVE`, `BLOCKED`), met `lastLoginAt` (platformbeheerder) |
| POST | `/admin/users` | Account aanmaken `{email, displayName, password?, platformRole}`; zonder wachtwoord gaat er een uitnodiging per e-mail uit (vereist `MAIL_PROVIDER`) |
| POST | `/admin/users/{id}/password-code` | `204`: stuurt een code van 72 uur om een nieuw wachtwoord in te stellen; het huidige wachtwoord blijft werken tot de code is gebruikt |
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
