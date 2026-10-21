# Learning log

Academic notes for the hospital bed dashboard. Each checkpoint records what was implemented and why.

## Checkpoint 1 — Repository initialization

The workspace was empty and the GitHub repository `Pradhyumnan22/Hospital-Management` existed with no commits. The monorepo lives at the workspace root (`frontend`, `backend`, `database`, `docs`) so the opened folder is the project.

Decisions:

- One Spring Boot service and one React app. No microservices, Redis, WebSockets, JWT, or a separate Python process.
- PostgreSQL is the application database. Flyway owns the schema. Automated tests use H2 in PostgreSQL mode so CI does not require Docker.
- Demo passwords exist only in the development/test seeder and in this log's companion README. They are not production credentials.
- Recommendations and forecasts are explainable deterministic Java. Nothing in this repository claims a trained model.

## Checkpoint 2 — Backend setup

Spring Boot is started with the Maven wrapper so a global Maven install is unnecessary. `application-dev.yml` points at local PostgreSQL through environment variables, with local-only defaults. `ddl-auto` is `validate` so Hibernate checks entities against Flyway instead of creating tables.

## Checkpoint 3 — Schema

Core tables: `app_user`, `patient`, `ward`, `bed`, `admission`, `audit_event`, `notification`. One active admission per patient is enforced with a unique `active_patient_key` that is cleared on discharge. Bed codes are unique within a ward. Bed states are `AVAILABLE`, `OCCUPIED`, `CLEANING`, and `OUT_OF_SERVICE`.

## Checkpoint 4 — Authentication

Server-side sessions and an HTTP-only session cookie. CSRF is a double-submit cookie (`XSRF-TOKEN` / `X-XSRF-TOKEN`) without the XOR masking handler, so the browser script can echo the cookie value. Passwords are stored with BCrypt. Role checks are enforced in Spring Security, not only in the UI.

## Checkpoint 5 — Wards and beds

Ward and bed changes are limited to administrators. Manual state changes cannot mark a bed occupied or start cleaning; those transitions happen only through admission and discharge. Turning a cleaned bed back to `AVAILABLE`, or taking a bed out of service, is a manual transition.

## Checkpoint 6 — Patients, admissions, discharges

Admissions run in a transaction that takes a pessimistic write lock on the bed and rechecks state. A second active admission for the same patient fails on the unique key. Discharge sets the bed to `CLEANING` and keeps the admission row.

## Checkpoint 7 — Recommendations

The engine filters on hard rules first (availability, required ward type, isolation, ventilator, cardiac monitor, oxygen). Only eligible beds are scored. Scores add or subtract documented points for preferred ward, isolation-bed preservation, and ward occupancy. The result lists exclusions and per-bed explanations. Staff confirmation calls the admission transaction, which runs the hard rules again.

## Checkpoint 8 — Occupancy and forecasting

Operational beds exclude `OUT_OF_SERVICE`. Occupancy is occupied divided by operational beds. The forecast averages net flow over the last 14 days, including days with zero activity, then draws a straight line for 24 hours and 7 days. Results are clamped to `[0, operational beds]`. Empty history yields a flat, low-confidence forecast. This ignores seasonality, time of day, and scheduled procedures.

## Checkpoint 9 — Frontend foundation

Vite proxies `/api` and sends cookies. A small fetch wrapper attaches the CSRF header and parses the shared error JSON. Auth state is a React context. Routing uses React Router. TypeScript strict mode is on, and `any` is not used.

## Checkpoint 10 — Patient and bed screens

Patient registration, detail with admission history, ward editing for administrators, and a color-coded bed grid are backed by the API.

## Checkpoint 11 — Dashboard, polling, notifications

Overview KPIs, ward occupancy, and forecast charts read analytics endpoints. Visible pages refresh every 20 seconds and stop the timer when the tab is hidden. Capacity alerts are stored as notifications.

## Checkpoint 12 — Tests and CI

Backend tests cover eligibility, ranking, forecasting boundaries, authentication, authorization, and competing allocation. Frontend tests cover the login form, the API client, and recommendation rendering. GitHub Actions runs both builds.

## Checkpoint 13 — Verification

Documented local PowerShell commands, demo accounts, and forecast limitations. Pushes are recorded only after `git push` succeeds.
