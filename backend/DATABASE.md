# Database

PostgreSQL 17 (lokaal en in CI; 16 werkt ook). Het schema wordt alleen door Liquibase beheerd (D-052):
SQL-changelogs in `src/main/resources/db/changelog/migrations/` (`NNN_omschrijving.sql`, één changeset per bestand),
in volgorde opgenomen door `db.changelog-master.yaml`. Hibernate valideert bij het starten dat de entiteiten erbij
passen. Een toegepaste changeset wordt nooit aangepast: wijzigingen gaan via een nieuw bestand. Liquibase houdt in
`databasechangelog` bij wat is uitgevoerd.

Conventies: UUID-sleutels (door de applicatie gegenereerd), enkelvoudige tabelnamen, `TIMESTAMPTZ` in UTC,
benoemde constraints (`pk_`, `fk_`, `uk_`, `ix_`, `ck_`), statuswaarden als tekst met een `CHECK`.

## Tabellen

| Tabel | Inhoud | Belangrijkste constraints |
|---|---|---|
| `app_user` | Accounts, platformrol (`PLATFORM_ADMIN`, `USER`), status, laatste login met wachtwoord (`last_login_at`, 018) | E-mail uniek en verplicht in kleine letters; wachtwoord alleen als adaptieve hash |
| `refresh_token` | Refresh tokens als SHA-256-hash, per loginfamilie | Hash uniek; vervaldatum na aanmaak |
| `mosque` | De tenant: slug, naam, adres, contact, tijdzone, status (`ACTIVE`, `INACTIVE`, `BLOCKED`), logo en omslagfoto | Slug uniek en `^[a-z0-9]+(-[a-z0-9]+)*$`; logo en omslag verwijzen naar `image` (`ON DELETE SET NULL`) |
| `mosque_settings` | Configuratie per moskee (017): aanmelden en push aan of uit | PK en FK op `mosque` (`ON DELETE CASCADE`); bestaande moskeeën kregen een rij met alles aan |
| `mosque_administrator` | Rol per gebruiker per moskee (`ADMIN`, `EDITOR`) | Uniek per (moskee, gebruiker) |
| `event` | Activiteiten | `ends_at > starts_at`; capaciteit > 0 en alleen met aanmelding; gepubliceerd heeft `published_at` |
| `event_registration` | Aanmeldingen (`CONFIRMED`, `CANCELLED`) | Uniek per (activiteit, gebruiker); FK op (event_id, mosque_id) |
| `media_item` | Verwijzing naar een externe video (`YOUTUBE` + video-ID) | Video uniek per moskee; gerelateerde activiteit via FK op (event_id, mosque_id) |
| `announcement` | Aankondigingen met optionele vervaldatum | Gepubliceerd heeft `published_at` |
| `mosque_follow` | Wie welke moskee volgt | PK (gebruiker, moskee) |
| `notification_preference` | Afwijkende voorkeur per onderwerp (`ANNOUNCEMENTS`, `EVENTS`, `MEDIA`) | FK op de volgrelatie met `ON DELETE CASCADE` |
| `push_device` | Pushtoken van een geïnstalleerde app: gebruiker, moskee van de app, platform | Token uniek; `mosque_id` (V13) mag alleen leeg zijn bij tokens van vóór fase 7, die nooit worden aangeschreven |
| `push_notification` | Verstuurde pushmelding per content-item met aantallen | Uniek per content-item; aantallen kloppen met elkaar |
| `audit_log` | Belangrijke beheeractie: wanneer, wie, welke moskee, actie, soort en ID van het object (015) | Actie in hoofdletters; `actor_user_id` wordt leeg als het account verdwijnt; na `RETENTION_AUDIT_LOG` (standaard 730 dagen) verwijderd |
| `image` | Geüploade afbeelding: moskee, type (`image/jpeg`, `image/png`), afmetingen; het bestand staat in de afbeeldingenmap | Hoort bij precies één moskee (`ON DELETE CASCADE`); een moskee kan alleen eigen afbeeldingen koppelen |

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
- Een pushtoken hoort bij de app van één moskee (`push_device.mosque_id`); versturen selecteert op die moskee, zodat
  een melding van moskee B nooit in de app van moskee A verschijnt.
- Indexen op `(mosque_id, status, starts_at)` en `(mosque_id, status, published_at)` dienen de publieke lijsten.
- Een afbeelding (`image`) hoort bij één moskee; koppelen als logo of omslag controleert dat de afbeelding van
  dezelfde moskee is (`IMAGE_NOT_FOUND` anders, getest in `ImageFlowIT`).

## Wat er niet in staat

Geen videobestanden, afbeeldingen of andere binaire data (geen `bytea`-kolommen; `DatabaseConstraintsIT` controleert
dat; afbeeldingen staan als bestand in de afbeeldingenmap, `STORAGE_DIRECTORY`), geen
wachtwoorden of refresh tokens in leesbare vorm, geen sleutels of instellingen van pushproviders (die staan in de
omgeving, zie DEPLOYMENT.md).
