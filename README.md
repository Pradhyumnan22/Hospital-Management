# AI Hospital Management and Bed Occupancy Dashboard

Hospital bed-management system for admissions, occupancy, and bed allocation. Bed recommendations are advisory and are applied only after an authorized staff member confirms allocation. The bundled patient rows are generated records from MITRE's public Synthea sample, so the database does not contain real patient information.

## Stack

- Frontend: React, Vite, TypeScript (strict), Tailwind CSS, Recharts, React Router, Lucide
- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Security (HTTP-only session cookies), Bean Validation, Flyway, PostgreSQL
- Tests: JUnit 5, Mockito, MockMvc, Vitest, React Testing Library

Recommendation ranking and occupancy forecasts are deterministic Java rules and rolling averages. This project does not train or call a machine-learning model.

## Prerequisites

- JDK 21
- Node.js 20 or newer
- PostgreSQL 15 or newer (local). Maven is downloaded by the wrapper (`mvnw`).

On this workstation PostgreSQL 18 was observed listening on port **6969** rather than 5432. Set `DB_URL` accordingly.

## Database setup (Windows PowerShell)

```powershell
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
& $psql -U postgres -h 127.0.0.1 -p 6969 -c "CREATE USER hospital WITH PASSWORD 'hospital';"
& $psql -U postgres -h 127.0.0.1 -p 6969 -c "CREATE DATABASE hospital_bed OWNER hospital;"
$env:DB_URL = "jdbc:postgresql://localhost:6969/hospital_bed"
```

Port 5432 is the usual PostgreSQL port. This workstation listens on **6969**, so the commands above use 6969. Development defaults in `backend/src/main/resources/application-dev.yml` still use 5432 until `DB_URL` is set.

Schema is applied by Flyway when the backend starts. Initial staff accounts are created only for the `dev` and `test` profiles. Clinical rows in `V3__synthea_sample.sql` come from the public [Synthea 100-patient CSV](https://github.com/synthetichealth/synthea-sample-data). Social security numbers are not imported. Visit times are placed across the last three weeks so the forecast chart has history. Cite Synthea as Walonoski et al., JAMIA, 2018, https://doi.org/10.1093/jamia/ocx079.

## Staff accounts

Created by the development seeder when the backend starts with the `dev` profile. Change these passwords before any deployment outside this machine.

| Username   | Password         | Role            |
|------------|------------------|-----------------|
| admin      | AdminDemo#2026   | ADMIN           |
| admission  | AdmitDemo#2026   | ADMISSION_STAFF |
| clinical   | ClinicDemo#2026  | CLINICAL_STAFF  |

## Run locally

`JAVA_HOME` must point at JDK 21. `scripts/start-backend.ps1` fills it when a known local JDK 21 directory exists.

```powershell
.\scripts\start-backend.ps1
```

In a second terminal:

```powershell
.\scripts\start-frontend.ps1
```

Or start each app from its own directory with `backend\mvnw.cmd spring-boot:run` and `npm run dev`.

Open `http://localhost:5173`. The Vite dev server proxies `/api` to `http://localhost:8080`.

## Tests and builds

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm install
npm test
npm run lint
npm run build
```

Backend tests use an in-memory H2 database in PostgreSQL compatibility mode so they do not need a running PostgreSQL server.

## Authorization

- Sessions use an HTTP-only cookie. CSRF uses a readable `XSRF-TOKEN` cookie and the `X-XSRF-TOKEN` header.
- `ADMIN` manages staff, wards, and beds, and can admit or discharge.
- `ADMISSION_STAFF` registers patients and confirms admissions and discharges.
- `CLINICAL_STAFF` can review patients, beds, recommendations, occupancy, and alerts.

Hard clinical eligibility rules are never overridden by the recommendation score. Confirming a recommendation locks the bed row and checks availability again inside the transaction.

## Forecasting

Forecasts use a 14-day rolling average of net daily flow (admissions minus discharges), extrapolated and clamped to operational capacity. They are labeled as predictions. See `docs/forecasting.md`.

## Troubleshooting

- **Port 5432 connection refused:** check the real listen port (`postgresql.conf` / `Get-NetTCPConnection`) and set `DB_URL`.
- **Password authentication failed:** create the `hospital` role as shown above, or set `DB_USERNAME` and `DB_PASSWORD`.
- **401 after login in the browser:** use the Vite origin so the session cookie is sent, and allow credentials. Do not call the API from a different site without updating CORS.
- **Bed already allocated:** the second confirmation returns HTTP 409. Refresh the bed grid and request recommendations again.
