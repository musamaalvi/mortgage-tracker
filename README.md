# Mortgage Tracker

Track mortgage payments, see where each payment goes (rent/interest vs equity), and forecast the next payment.

- **Backend:** Java 21, Spring Boot 3, Spring Security, JPA + Flyway, Apache POI (`backend/`)
- **Frontend:** React + Vite + TypeScript, Recharts (`frontend/`)
- **Database:** PostgreSQL 16, data stored in `./postgres-data` (bind mount, git-ignored)

## Run

```bash
cp .env.example .env     # then set the passwords
docker compose up -d --build
```

Open http://localhost:3000 and sign in with `APP_USERNAME` / `APP_PASSWORD` from `.env`.

| `.env` key | Purpose |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database credentials |
| `POSTGRES_PORT` | Host port for a DB client (127.0.0.1 only, default 5433) |
| `APP_USERNAME`, `APP_PASSWORD` | App login |
| `APP_PORT`, `BACKEND_PORT` | Host ports for the UI (3000) and API (8080) |

Changing `APP_*` needs `docker compose up -d backend` to take effect. Changing the `POSTGRES_*` credentials after the first start does nothing, because Postgres keeps the credentials it was created with in `./postgres-data`.

To wipe all data: `docker compose down && rm -rf postgres-data`.

## Develop

```bash
docker compose up -d db backend      # API on :8080
cd frontend && npm install && npm run dev   # UI on :5173, proxies /api to :8080
```

Backend tests (no local JDK needed):

```bash
docker run --rm -v "$PWD/backend":/app -v mortgage-tracker-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn -B test
```

## How it works

- **Payments**: `total = rent (interest) + equity (principal)`. A mismatch is allowed but flagged ⚠.
  Several payments can share a date; mark extra ones as *Overpayment*.
- **Excel import**: first sheet, from row 2. A = date (`dd/mm/yyyy` text or an Excel date), B = total,
  C = rent, D = equity. Empty B–D cells count as 0. Import stops at the first empty row, and rows that
  can't be read are skipped and reported. Rows with zero rent are marked as overpayments.
  There's no duplicate check, so importing the same file twice adds the rows twice.
- **Remaining balance** = mortgage amount − all equity paid.
- **Interest rates** are kept as a history with *effective from* dates. The rate in effect on each date is used.
- **Expected next payment**: standard repayment formula on the remaining balance, at the current rate (annual ÷ 12),
  over the payments left in the term. The *payoff date* assumes you keep paying your latest regular payment amount.
