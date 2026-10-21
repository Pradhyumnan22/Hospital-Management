# AI Hospital Management and Bed Occupancy Dashboard

Academic demonstration of a hospital bed-management workflow. Patient records in this project are synthetic. Bed recommendations are advisory and are applied only after an authorized staff member confirms allocation.

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
& $psql -U postgres -h 127.0.0.1 -p 5432 -c "CREATE USER hospital WITH PASSWORD 'hospital';"
& $psql -U postgres -h 127.0.0.1 -p 5432 -c "CREATE DATABASE hospital_bed OWNER hospital;"
```

Change the port if your server is not on 5432. Development defaults are in `backend/src/main/resources/application-dev.yml` and are for local use only.

Schema is applied by Flyway when the backend starts. Demo users and synthetic clinical rows are created only for the `dev` and `test` profiles.

## Demo accounts

Created by the development seeder. Do not reuse these passwords outside a local demo.

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
