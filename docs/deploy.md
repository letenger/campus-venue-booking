# Running and deploying

Three ways to run this, from "just look at it" to "a real server with a real database".

## 1. Local, full stack

Two terminals. The backend needs Java 25; the frontend needs Node 20+.

```bash
cd backend
./gradlew bootRun          # http://localhost:8080, H2 in memory
```

```bash
cd frontend
npm install
npm run dev                # http://localhost:5173, proxies /api to :8080
```

Open <http://localhost:5173> and sign in with `student` / `student123`,
`teacher` / `teacher123` or `admin` / `admin123`. The database is rebuilt and
re-seeded on every restart, so anything you book is gone after a restart — that
is the point of the dev profile.

Check the API on its own:

```bash
curl -s localhost:8080/api/venues | head -c 200
curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"student123"}'
```

## 2. Static demo (what a Vercel link is)

The API is Spring Boot, and Vercel runs neither Java nor a long-lived process.
So the public preview is the *same* Vue app built with a different data source:

```bash
cd frontend
npm run build:demo         # VITE_DEMO=1, hash routes, in-memory API
```

`src/demo/index.js` replaces the axios adapter with an in-memory implementation of
every endpoint — same envelopes, same status codes, same rules (409 on overlap,
approval for clubs and groups, the scheduler's no-show rule). Everything you can
click is the real UI; only the persistence is a JavaScript object in
`localStorage`.

That build is a folder of static files, so it goes anywhere:

```bash
npx vercel --prod          # from frontend/, vercel.json is already set up
```

`vercel.json` pins the demo build command, the output directory and the SPA
rewrite. Pushing the repo to GitHub and importing it in Vercel works the same
way, with **Root Directory = `frontend`**.

What the demo does *not* do: no real authentication, no shared state between
visitors, no audit trail. It is a preview, not a deployment of the product.

## 3. Full stack, split hosting

The honest production shape: static client on a CDN, Java API on a container
host, MySQL for storage.

```
Vercel (frontend)  ──HTTPS──▶  Render (backend, Docker)  ──▶  MySQL
```

### Backend

`backend/Dockerfile` builds a jar on JDK 25 and runs it on a JRE. `render.yaml`
at the repo root describes the service. Any container host works — Render,
Railway, Fly.io, a school VM with Docker.

```bash
docker build -t campus-venue-api ./backend
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL='jdbc:mysql://host:3306/campus_venue?useSSL=false&serverTimezone=UTC' \
  -e DB_USER=campus -e DB_PASSWORD=secret \
  -e JWT_SECRET='a-long-random-string' \
  campus-venue-api
```

The `prod` profile validates the schema instead of creating it, serves no H2
console, and refuses to start without `JWT_SECRET`.

### Frontend against a remote API

Point the client at the API host at build time:

```bash
VITE_API_BASE=https://campus-venue-api.onrender.com/api npm run build
```

Then allow that origin on the backend:

```
APP_CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

Spring maps that environment variable onto `app.cors.allowed-origins`.

## Configuration reference

| Variable | Default | What it does |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | port the API listens on |
| `SPRING_PROFILES_ACTIVE` | dev | `prod` switches to MySQL and schema validation |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | — | MySQL connection, prod only |
| `JWT_SECRET` | dev value | signing key; required in prod |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | one origin allowed to call the API |
| `VITE_API_BASE` | `/api` | where the client looks for the API |
| `VITE_DEMO` | off | `1` swaps the API for the in-memory one |

## What is still missing before this is a real school deployment

- **Schema migrations.** `ddl-auto` is `create-drop` in dev and `validate` in
  prod, so an existing database must be created by hand. Flyway with one
  baseline migration is the next step.
- **The overlap race.** Two simultaneous requests for the same slot can both
  pass the check. A unique index on `(venue_id, booking_date, start_time)` plus
  `@Version` closes it.
- **Backups and retention.** Nobody wants to lose a term of booking history.
- **Single sign-on.** A school already has accounts; this app issues its own.
