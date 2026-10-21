# Database

Flyway migrations live in `backend/src/main/resources/db/migration` and are the source of truth. This folder records how to create the local database.

```powershell
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
& $psql -U postgres -h 127.0.0.1 -p 5432 -c "CREATE USER hospital WITH PASSWORD 'hospital';"
& $psql -U postgres -h 127.0.0.1 -p 5432 -c "CREATE DATABASE hospital_bed OWNER hospital;"
```

If the server uses another port, substitute it in both commands and in `DB_URL`.

The `hospital` / `hospital` pair is a local development default. Use a different password on any shared machine and set `DB_PASSWORD`.
