# Choir App (nofomo) — Backend

Membership and attendance management for the choir. Members check in to rehearsal sessions with
tickets; a doorman scans QR codes or checks people in by name; an admin manages members, tickets
and session finalization.

**Live:** https://lind-horst.com/nofomo/
**Stack:** Spring Boot 4 · Java 21 · Postgres 15 · Docker Compose (Caddy + Spring + Postgres).
Frontend lives in `choir-vue-frontend` (see its `README.md`).

> Companion docs: `choir-vue-frontend/README.md` (frontend guide) and `TODO.md` in this folder
> (all open tasks). This file supersedes the former `ANALYSIS.md`, `REVIEW-2026-07-08.md`,
> `GOLIVE-TODO.md`, the dated `TODO-*.md` files, `db_dump.txt` and `general-idea.md` — their
> content is folded into these three docs (git history retains the originals).

## How it works

- **Three roles, one login page** (`/login`), one shared password per role, cumulative rights:
  `member` < `doorman` < `admin`. Session-cookie auth (Spring Security), login is rate-limited
  (15 failed attempts per IP / 15 min, in-memory `LoginAttemptService`). CSRF is on
  (`CookieCsrfTokenRepository`; the frontend echoes the `XSRF-TOKEN` cookie as `X-XSRF-TOKEN`).
- **Members** identify themselves via a personal secret key (`/member/<secretKey>`), shown as a
  QR code they can save/print. Check-in consumes a ticket (commit tickets first, then regular).
  A member may go up to 3 tickets into debt (`Member.MINIMUM_TICKET_COUNT = -3`).
- **Sessions** open automatically on the first check-in of the evening and are finalized by the
  admin as `REGULAR_ONLY`, `COMMIT` (absent members holding commit tickets are charged a no-show
  penalty), or `FREE` (every attendee is refunded 1 regular ticket, regardless of which ticket
  type check-in actually deducted). Forgotten sessions are auto-closed nightly at 03:00
  (`SessionCleanupJob`, `MAX_SESSION_AGE = 12h`) and the Chorleiter gets a reminder email.
- **Member dashboard** has a Schedule card backed by `GET /member/schedule/ics`
  (`ScheduleService`): fetches the choir's iCloud `webcal://` feed server-side (converted to
  `https://`), cached in memory for 10 minutes, rendered client-side with FullCalendar. Feed URL
  is `app.schedule-ics-url` / `SCHEDULE_ICS_URL` below.
- **Ticket ledger** — every balance change writes an append-only `TicketTransaction` row
  (`TicketLogService`: Init/initial balance, Admin top-up, Check-in, Commit no-show charge, Free
  refund; signed deltas per ticket type, optional session link). Members see their full history in
  a "Ticket Log" card; the admin sees the same log in the member detail modal. On the first start
  with an empty ledger, `TicketLogBackfill` snapshots every existing member's balance as Init rows
  (one per non-zero ticket type) — after that it never runs again.
- **Members are archived, not deleted** — an `archived` flag (admin member modal) hides them from
  the default admin table while keeping their attendance and ticket history.
- **Feedback form** (footer link, visible after login): `POST /api/contact` (authenticated,
  `ContactService`) emails name/email/message to `ADMIN_FEEDBACK_EMAIL`, reply-to set to the
  sender, with debug context attached (page URL, member key if present, logged-in role, viewport,
  user agent, client+server timestamps).

## Data model

Four tables (Hibernate `ddl-auto=update` — see the enum-constraint caveat under *Good to know*):

- **`members`** — `id`, `name` (unique), `secret_key` (unique, e.g. `red-abba-13`; generated from
  colour + artist wordlists via `SecureRandom` in `MemberKeyGeneratorService`), `regular_tickets`,
  `commit_tickets`, `archived`.
- **`sessions`** — `id`, `start_time`, `is_open`, `session_type`
  (`NONE` → `AUTO_CLOSE` / `REGULAR_ONLY` / `COMMIT` / `FREE`).
- **`attendance`** — `id`, `checkin_time`, `status` (`PRESENT` / `NO_SHOW`), `member_id`,
  `session_id`.
- **`ticket_transactions`** — the append-only ticket ledger: `id`, `timestamp`, `regular_delta`,
  `commit_delta`, `type` (`INITIAL_BALANCE` / `ADMIN_ADJUSTMENT` / `CHECK_IN` / `NO_SHOW_CHARGE` /
  `FREE_SESSION_REFUND`), `member_id`, optional `session_id`. Rows are only ever inserted.

## API surface

Access is enforced by role in `SecurityConfig` (`/admin/**` ADMIN, `/doorman/**` DOORMAN,
`/member/**` MEMBER, `/api/contact` any authenticated, other `/api/**` public).

| Method & path | Role | Purpose |
|---|---|---|
| `GET /api/me` | public | current auth state + roles |
| `POST /api/login` · `POST /api/logout` | public | Spring Security form login/logout |
| `POST /api/contact` | any | feedback email |
| `GET /member/{secretKey}` | member | member info (tickets, attendance, checked-in) |
| `GET /member/{secretKey}/ticketlog` | member | full ticket ledger for the member |
| `POST /member/checkin/{secretKey}` | member | self check-in |
| `GET /member/schedule/ics` | member | cached iCloud calendar feed |
| `GET /doorman/members` | doorman | member names (for the search dropdown) |
| `GET /doorman/checkedin` | doorman | names currently checked in |
| `POST /doorman/checkin/{id}` | doorman | check in a member by id |
| `GET /admin/sessions` | admin | sessions + attendee counts |
| `GET /admin/sessions/members/{id}` | admin | attendees of a session |
| `POST /admin/finalizeSession` | admin | finalize (`EndSessionRequest`) |
| `GET /admin/members` | admin | all members incl. secret keys |
| `GET /admin/members/{id}/ticketlog` | admin | full ticket ledger for one member |
| `POST /admin/member` | admin | create member (`CreateMemberRequest`) |
| `POST /admin/members/archive` | admin | archive/reactivate (`ArchiveMemberRequest`) |
| `POST /admin/tickets` | admin | add tickets (`AddTicketsRequest`) |

## Repos & architecture

| Repo | Contents |
|---|---|
| [choir-backend](https://github.com/Minthorst/choir-backend) | Spring Boot 4 / Java 21 API, Postgres, docker-compose (the whole deployment lives here) |
| [choir-frontend](https://github.com/Minthorst/choir-frontend) | Vue 3 / Vite SPA + Caddyfile + Dockerfile (built into the proxy image) |

Three containers, orchestrated by `docker-compose.yml` in this repo:

```
Browser ── https://lind-horst.com/nofomo/ ──► caddy (choir_caddy, ports 80+443)
                                                │  serves the Vue build (/srv/nofomo)
                                                │  auto-manages Let's Encrypt certificates
                              /nofomo/api/* ────┤  strips prefix, proxies to
                                                ▼
                                              app (choir_backend, internal :8080)
                                                ▼
                                              db (choir_db, Postgres 15, internal :5432)
```

Only Caddy publishes ports. Backend and DB are unreachable from the internet.
Images are prebuilt by GitHub Actions and pulled from GHCR — the server never compiles.

## Deployment

- **Every push** to `master` (backend) / `main` (frontend) triggers the *Build and Deploy*
  workflow, which runs the test suite (`mvn test` / `npm test`) and then builds the Docker image
  and pushes it to GHCR. This is also the CI gate — a broken build or a red test goes red and
  never reaches the server.
- **Deploying is manual:** GitHub → Actions → *Build and Deploy* (left sidebar) → **Run
  workflow**. The run builds the latest commit, then SSHes into the VPS and runs the update.
- **Manual fallback** (identical to what the workflow does):
  ```bash
  ssh jan@87.106.1.20
  cd ~/apps/choir-backend
  git pull && docker compose pull && docker compose up -d --no-build
  ```

GitHub Actions secrets (set in **both** repos): `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`
(private half of the dedicated deploy key `~/.ssh/choir_deploy`; its public half is in the
server's `authorized_keys`).

## Server

IONOS VPS S+ (2 vCores / 2 GB RAM / Debian 13) at `87.106.1.20`, DNS: A record for
`lind-horst.com`.

```bash
ssh jan@87.106.1.20        # key-based only; root login and password auth are disabled
```

Setup that exists on the box: ufw (22/80/443 only), fail2ban, unattended-upgrades, 2 GB swap
file, Docker. The app lives in `~/apps/choir-backend` (git clone + `.env`). There is **no**
frontend checkout on the server — its image comes from GHCR. Uptime is monitored by UptimeRobot
against the live URL.

## Logs

```bash
ssh jan@87.106.1.20
cd ~/apps/choir-backend

docker compose logs -f            # everything, interleaved
docker compose logs -f app        # backend (Spring)
docker compose logs -f caddy      # proxy: access log, certificate events
docker compose logs -f db         # postgres
docker compose logs --tail 100 --since 1h -t app   # useful flags: tail / since / timestamps
```

Business events (check-ins, finalizations, session open/auto-close, member/ticket changes,
feedback mails) are logged at INFO by the services. Unexpected backend errors show users a red
modal with a **Fehler-ID**; find the full stack trace with:

```bash
docker logs choir_backend 2>&1 | grep <fehler-id>
```

One-liner without an interactive session:
`ssh jan@87.106.1.20 'docker logs choir_backend --tail 50'`

## Database console

```bash
ssh jan@87.106.1.20
docker exec -it choir_db psql -U postgres -d choir
```

Useful bits:

```sql
\dt                                            -- list tables (members, sessions, attendance)
SELECT id, name, regular_tickets, commit_tickets FROM members ORDER BY name;
SELECT * FROM sessions ORDER BY start_time DESC LIMIT 5;
SELECT m.name, a.checkin_time, a.status FROM attendance a
  JOIN members m ON m.id = a.member_id ORDER BY a.checkin_time DESC LIMIT 20;
\q                                             -- exit
```

⚠️ This is the live database — `SELECT` freely, but think twice before `UPDATE`/`DELETE`
(there is no undo; prefer doing changes through the admin UI where possible).

For a GUI (IntelliJ database tool) against the **live** DB: the port is deliberately not
published on the server, and a plain SSH tunnel can't reach into the container. Use `psql` in
the container (above), or — for a one-off GUI session — temporarily publish the port:

```bash
# on the server: forward host port 5433 to the db container, bound to localhost only
docker run --rm -d --name pgtunnel --network choir-backend_default -p 127.0.0.1:5433:5432 \
  alpine/socat tcp-listen:5432,fork,reuseaddr tcp:db:5432
# on the Mac: ssh -L 5433:localhost:5433 jan@87.106.1.20 → connect IntelliJ to localhost:5433
# afterwards on the server: docker stop pgtunnel
```

Against the **local dev** DB no tricks are needed — `localhost:5432` is published via
`docker-compose.override.yml`.

## Database backups

Nightly automated backups run on the server via cron + `~/apps/choir-backend/backup-db.sh`
(tools: `pg_dump`/`psql` inside the `choir_db` container, `rclone` remote `GDriveJan` →
Google Drive, `drive.file` scope).

Every night at 04:00 the script:

1. Dumps the live `choir` DB with `pg_dump --clean --if-exists` (self-contained; safe to restore
   over existing data because it drops tables before recreating them).
2. Uploads the dump to `GDriveJan:choir-backups/`.
3. On the 1st of each month additionally keeps a permanent copy in `db-backups/monthly/` on the
   server and `GDriveJan:choir-backups/monthly/` on Drive (never auto-deleted).
4. Prunes local dumps older than 30 days. Drive copies are **not** auto-pruned — clean up by hand
   occasionally, but always keep `monthly/`.

Cron entry (`crontab -l`):

```
0 4 * * * /home/jan/apps/choir-backend/backup-db.sh >> /home/jan/apps/choir-backend/db-backups/backup.log 2>&1
```

**Manual backup** — full script run: `~/apps/choir-backend/backup-db.sh`, or a one-off dump
without Drive upload/pruning:

```bash
docker exec choir_db pg_dump -U postgres --clean --if-exists -d choir > ~/apps/choir-backend/db-backups/manual_$(date +%F_%H%M).sql
```

**Restore** (local dump on the server):

```bash
docker exec -i choir_db psql -U postgres -d choir < /path/to/the/dump.sql
# verify:
docker exec -i choir_db psql -U postgres -d choir -c "SELECT COUNT(*) FROM members;"
docker exec -i choir_db psql -U postgres -d choir -c "SELECT COUNT(*) FROM sessions;"
docker exec -i choir_db psql -U postgres -d choir -c "SELECT COUNT(*) FROM attendance;"
```

**Restore from Google Drive:**

```bash
rclone ls GDriveJan:choir-backups/            # see what's available (also: .../monthly/)
rclone copy GDriveJan:choir-backups/monthly/choir_2026-07.sql ~/apps/choir-backend/db-backups/
docker exec -i choir_db psql -U postgres -d choir < ~/apps/choir-backend/db-backups/choir_2026-07.sql
```

⚠️ **Don't mix up dump vs restore:** `pg_dump ... > file.sql` *writes* the file (backup);
`psql ... < file.sql` *reads* it (restore). Never reuse the same filename for a backup you're
about to take and a restore you're about to run in one session — the backup command silently
overwrites the file first.

## Environment variables

All secrets live in `~/apps/choir-backend/.env` on the server (chmod 600, never in git; keep a
copy of the values in a password manager). Locally the same file exists with dev values.

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | Postgres password (db container + backend datasource) |
| `MEMBER_PASSWORD` | login password for the member role |
| `DOORMAN_PASSWORD` | login password for the doorman role |
| `ADMIN_PASSWORD` | login password for the admin role |
| `MAIL_USERNAME` | SMTP login (IONOS) and sender address for notification mails |
| `MAIL_PASSWORD` | SMTP password |
| `EMAIL_CHORLEITER` | recipient of the nightly "session not finalized" mail |
| `ADMIN_FEEDBACK_EMAIL` | recipient of the feedback-form mails (`/api/contact`) — ⚠️ required, a missing value prevents backend startup (compose default: `admin.nofomo@lind-horst.com`) |
| `FRONTEND_URL` | optional, default `https://lind-horst.com/nofomo` — base URL used in mails |
| `SITE_ADDRESS` | optional, Caddy site address; unset in prod (= real domain + HTTPS), `:80` locally via the override file |
| `SCHEDULE_ICS_URL` | optional, defaults to the choir's iCloud published-calendar `https://` URL — feed behind the Schedule card |

**Changing a value:**

```bash
ssh jan@87.106.1.20
nano ~/apps/choir-backend/.env        # edit
cd ~/apps/choir-backend
docker compose up -d                  # recreates only the affected containers
```

⚠️ `DB_PASSWORD` is special: Postgres stores its password in its data volume on first start, so
changing the variable later does **not** change the actual DB password — the backend would just
fail to connect. If it ever must change, change it inside Postgres too
(`ALTER USER postgres WITH PASSWORD '...'`) before restarting.

## Local development

```bash
# backend + db (+ prod-shaped caddy) — run from choir-backend/
docker compose up -d --build          # dev ports 8080/5432 come from docker-compose.override.yml

# frontend with hot reload — run from choir-vue-frontend/
npm run dev                           # → http://localhost:5173/nofomo/

# backend tests — no Docker/Postgres needed, they run against in-memory H2
mvn test                              # unit + MockMvc end-to-end suites
```

Test configuration lives in `src/test/resources/application.properties` (H2 datasource + dummy
values for every env-var placeholder). Note it fully *shadows* the main properties file on the
test classpath, so any new required property must be added there too.

`docker-compose.override.yml` is local-only (gitignored) — it republishes the internal ports for
IDE access and switches Caddy to plain HTTP. **Never copy it to the server.**

Running the backend from IntelliJ instead of Docker requires the env vars in the run
configuration (`SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/choir`,
`SPRING_DATASOURCE_USERNAME=postgres`, `SPRING_DATASOURCE_PASSWORD=...`, the role passwords, the
mail settings, and `ADMIN_FEEDBACK_EMAIL`).

## Good to know

- **`ddl-auto=update` does not widen enum `CHECK` constraints.** Hibernate generates a DB-level
  `CHECK` constraint from an `@Enumerated(EnumType.STRING)` field's values, but only when it first
  creates the table — adding a new enum constant later (as with `SessionType.FREE`) does not
  update it, so inserts using the new value fail until the constraint is widened by hand. A move to
  Flyway/Liquibase would catch this at migration time instead of at runtime (see `TODO.md`).
- **Config can fail-fast.** A missing required `@Value` env var stops backend startup with a
  "Could not resolve placeholder" error (this took prod down once when `ADMIN_FEEDBACK_EMAIL`
  wasn't set). Non-critical vars should carry defaults; see `TODO.md`.
- **`TicketLogBackfill` keys off "ledger table is empty".** Deploy the ledger change before
  creating new members in prod: a post-ledger `createMember` writes Init rows, the ledger is then
  non-empty, and existing members would never get their backfilled Init snapshot.
- These docs contain the VPS IP and SSH username. The repos are public — that's a deliberate
  trade-off (findable via DNS anyway), but **never** let actual secrets into them.
