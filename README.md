# Mijn Ummah

Platform voor moskee-apps. De eerste pilot is de app van **Noeroel Islam**: een mobiele app (Flutter),
een beheerportaal (Angular) en een gedeelde backend (Spring Boot, PostgreSQL). Elke moskee krijgt een eigen app uit
dezelfde code: zie [WHITE_LABEL.md](WHITE_LABEL.md).

| Map | Inhoud |
|---|---|
| [`backend/`](backend/README.md) | Spring Boot-API en PostgreSQL-migraties |
| `frontend/app/` | Flutter-app |
| `frontend/webportaal/` | Angular-beheerportaal |
| [`brands/`](WHITE_LABEL.md) | Merk per moskee-app: naam, ID's, kleuren, beeldmerk |
| [`tools/brand/`](WHITE_LABEL.md#commandos) | Merken controleren, aanmaken en toepassen |
| [`design/`](design/README.md) | Design tokens, fonts, logo's en iconen |
| [`deploy/`](DEPLOYMENT.md) | Productie-compose, back-up en herstel |

De repository is nog in opbouw. Zie [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) voor de fases en de
voortgang, en [DECISIONS.md](DECISIONS.md) voor de architectuur- en ontwerpbeslissingen. Privacy en AVG:
[PRIVACY.md](PRIVACY.md). Productie: [DEPLOYMENT.md](DEPLOYMENT.md).
