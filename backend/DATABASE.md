# Database

PostgreSQL 17 (lokaal en in CI; 16 werkt ook). Het schema wordt alleen door Flyway beheerd
(`src/main/resources/db/migration`); Hibernate valideert bij het starten dat de entiteiten erbij passen. Een
toegepaste migratie wordt nooit aangepast: wijzigingen gaan via een nieuwe migratie. `flyway clean` is uitgeschakeld.

Conventies: UUID-sleutels (door de applicatie gegenereerd), enkelvoudige tabelnamen, `TIMESTAMPTZ` in UTC,
benoemde constraints (`pk_`, `fk_`, `uk_`, `ix_`, `ck_`), statuswaarden als tekst met een `CHECK`.

## Tabellen

| Tabel | Inhoud | Belangrijkste constraints |
|---|---|---|
| `app_user` | Accounts, platformrol (`PLATFORM_ADMIN`, `USER`), status | E-mail uniek en verplicht in kleine letters; wachtwoord alleen als adaptieve hash |
| `refresh_token` | Refresh tokens als SHA-256-hash, per loginfamilie | Hash uniek; vervaldatum na aanmaak |
| `mosque` | De tenant: slug, naam, adres, contact, tijdzone, actief | Slug uniek en `^[a-z0-9]+(-[a-z0-9]+)*$` |
| `mosque_administrator` | Rol per gebruiker per moskee (`ADMIN`, `EDITOR`) | Uniek per (moskee, gebruiker) |
| `event` | Activiteiten | `ends_at > starts_at`; capaciteit > 0 en alleen met aanmelding; gepubliceerd heeft `published_at` |
| `event_registration` | Aanmeldingen (`CONFIRMED`, `CANCELLED`) | Uniek per (activiteit, gebruiker); FK op (event_id, mosque_id) |
| `media_item` | Verwijzing naar een externe video (`YOUTUBE` + video-ID) | Video uniek per moskee; gerelateerde activiteit via FK op (event_id, mosque_id) |
| `announcement` | Aankondigingen met optionele vervaldatum | Gepubliceerd heeft `published_at` |
| `mosque_follow` | Wie welke moskee volgt | PK (gebruiker, moskee) |
| `notification_preference` | Afwijkende voorkeur per onderwerp (`ANNOUNCEMENTS`, `EVENTS`, `MEDIA`) | FK op de volgrelatie met `ON DELETE CASCADE` |

Content-tabellen hebben auditkolommen (`created_at`, `created_by`, `updated_at`, `updated_by`) en een `version`
voor optimistic locking: als twee beheerders tegelijk hetzelfde item opslaan, krijgt de tweede
`409 CONCURRENT_MODIFICATION`.

## Tenantisolatie

- Elke moskeespecifieke tabel heeft een verplichte `mosque_id` met foreign key naar `mosque`.
- `event` heeft een unieke sleutel op `(id, mosque_id)`. Aanmeldingen en gerelateerde media verwijzen naar die
  combinatie, zodat de database zelf weigert dat een aanmelding of video naar een activiteit van een andere moskee
  wijst.
- In de applicatie zoekt elke beheeractie objecten op met `(id, mosque_id)`, nadat `MosqueAccessService` de
  gebruiker voor die moskee heeft goedgekeurd.
- Indexen op `(mosque_id, status, starts_at)` en `(mosque_id, status, published_at)` dienen de publieke lijsten.

## Wat er niet in staat

Geen videobestanden of andere binaire data (geen `bytea`-kolommen; `DatabaseConstraintsIT` controleert dat), geen
wachtwoorden of refresh tokens in leesbare vorm, geen gegevens van pushproviders (fase 5).
