# Open Tasks

Single consolidated backlog for both repos, merged from the former `GOLIVE-TODO.md`, `ANALYSIS.md`,
`REVIEW-2026-07-08.md` and the dated `TODO-*.md` files. Only **open** items are listed here —
everything already done (the whole go-live security/deploy refactor, the feedback form, doorman
invalid-QR/camera-error handling, `show-sql=false`, service logging, the styling/UX passes,
negative-ticket highlighting, password-manager support, finalized login passwords, DB backups with
our own rclone client_id, a verified restore-from-Drive, uptime monitoring, member archiving, the
ticket ledger incl. member/admin Ticket Log UI + Init backfill, the H2 test config, the backend
unit/e2e + frontend Vitest suites, and test steps in both CI workflows) has been dropped.
Legend: 🔴 do soon · 🟡 real but not urgent · 🟢 optional.

## 🔴 Operational / go-live

- [ ] **Go-live DB restore.** Restore the hand-edited `pre_niklas_*.sql` (with the 9 key renames —
      `rammstein→muse` ×6, `garbage→beck` ×3) onto the server, then verify the app against it. This
      is the "return to the real data before go-live" step.
- [ ] **Config fail-fast policy.** A missing `ADMIN_FEEDBACK_EMAIL` crash-looped prod once. Decide:
      sane defaults in `application.properties` for every non-critical `@Value`, or a validated
      `@ConfigurationProperties` class that fails with one clear message. Today it's mixed.
- [ ] **Docker log rotation.** The default `json-file` driver never rotates → can fill the VPS disk.
      Add an `x-logging` anchor (`max-size: 10m`, `max-file: 5`) and `logging: *default-logging` on
      each service in `docker-compose.yml`, then `docker compose up -d`.
- [ ] **Verify the nightly `SessionCleanupJob` email** actually fires with the fixed link — needs a
      real forgotten (unfinalized) session to occur; check after the first one.

## 🟡 Known bugs

- [ ] **`MemberDash.checkInAndFetchMember` loading state** — sets `loading=true` then synchronously
      `false`; the flag is meaningless for the request. Rewrite with `async/await` + `finally`.
- [ ] **Duplicate-open-session race** — two simultaneous first check-ins can create two open
      sessions (`findFirstByIsOpenWithLock` can't lock a non-existent row). Fix with a partial
      unique index `ON sessions (is_open) WHERE is_open = true` + retry-on-conflict.
- [ ] **Doorman QR "already checked in" still string-matches** the error message
      (`e.message.startsWith("Member is already checked in…")`). Give `POST /member/checkin/{key}`
      a `checkedIn` boolean like the by-id flow and branch on that. (Invalid-QR / camera-error
      handling is already done.) ⚠️ Do this before the language pass below.
- [ ] **`AttendanceView` `:key` uses `dateTime`** — collides for bulk NO_SHOW rows sharing one
      timestamp, breaking keying and the row numbering. Expose the attendance `id` in
      `AttendanceDTO` and key on it. Same idea in `TicketLogView` (composite
      timestamp+type+deltas key) — expose the transaction `id` there too.

## 🟡 Security / robustness

- [ ] **`/api/contact` hardening** — `ContactRequest` has only `@NotBlank`. Add `@Email` on `email`
      (it's the reply-to) and `@Size` caps (name, message) so a member can't mail huge bodies, plus
      a per-session/IP rate limit (reuse the `LoginAttemptService` pattern) since it triggers
      outbound SMTP.
- [ ] **Secret key leaks into logs.** The key travels in the URL path (`/member/{key}`) so it lands
      in Caddy access logs and browser history, and `ResourceNotFoundException` echoes it into the
      404 body/logs. Cheap win: stop echoing the key in error messages/logs. Longer-term: consider
      moving it to a header/body (bookmarkability is a deliberate feature, so document the choice).
- [ ] **Ticket debt — document + surface on the member side.** `Member.MINIMUM_TICKET_COUNT = -3`
      lets members go into debt; add a code comment explaining the rule, and show negative balances
      in red on the **member dashboard** too (the admin table + member modal already do).

## 🟢 Wording / inclusivity

- [ ] **One language.** UI copy, table headers, backend error messages (shown raw in red modals)
      and emails still mix English/German. Decide (presumably German) and do one pass. Kill
      "Hell Yeah"/"nope" in the member table. Do the QR string-match bugfix first.
- [ ] **Finish "Doorhuman".** Nav label + route are done; component/headings/role/package are still
      "Doorman". UI-labels-only is fine — just finish the visible headings.

## 🟢 Features (stretch)

- [ ] **Per-member voice-per-song map.** Store which voice/part each member sings for each song
      (Sopran/Alt/Tenor/Bass or a free label), settable in the admin member view (optionally
      member-visible). Needs a repertoire/`Song` list first, then a `member × song → voice`
      mapping.
- [ ] **Admin → member dashboard messages** — `AdminMessage` table + endpoint + a card on the
      member dash.
- [ ] **CSV export** of session attendee data.
- [ ] **Per-row QR/link copy** in the admin members table (QR generation now exists as an offline
      batch script; this would bring it into the UI).
- [ ] **Shrink `MAX_SESSION_AGE`** 12h → ~7h so forgotten-session mail arrives next morning, not a
      day later.
- [ ] **Doorman "Currently Checked In" auto-refresh** — poll ~30s or refresh on window focus so two
      door devices stay in sync (today it only refreshes after its own check-ins).
- [ ] **Login-page bug channel** — the Feedback button is only reachable *after* login, so a broken
      login has no in-app reporting path. Add a mailto fallback on the gate / announcement.

## 🟢 Code quality

- [ ] **`LoginAttemptService` window-logic tests** — the one money-adjacent class still without
      coverage (failure counting, 15-min window expiry, reset on success).
- [ ] **Flyway/Liquibase** — replace `ddl-auto=update` so schema/enum changes ship as reviewable
      migrations (the enum `CHECK`-constraint gotcha already bit `SessionType.FREE`).
- [ ] **`vue-tsc --noEmit` as a CI step** — blocked by ~18 pre-existing implicit-`any` errors;
      cheapest path is converting `router/index.js`, `router/pendingRedirect.js`, `main.js` to TS
      and adding `lang="ts"` to `Modal.vue` / `BaseButton.vue`.
- [ ] **ESLint + Prettier** config in both repos — nothing enforces style; quote/semicolon usage
      drifts file to file.
- [ ] **`usePagedSort` composable** — the sort+paginate logic is hand-copied four times
      (`AttendanceView`, `TicketLogView`, `AdminDash` sessions + members).
- [ ] **Split `AdminDash.vue`** (~500 lines) into `SessionsTable` / `MembersTable` / `AddMemberForm`
      / `TicketAddCell` (the last also removes the `ticketInputs` id-map hack).
- [ ] **Backend naming/mapping hygiene** — rename packages `Boundary` → `controller` + `dto` and
      `Exception` → `exception` (Java lowercase convention); extract a `MemberMapper` (the
      `//TODO move to mapper` in `MemberService` agrees); `MemberService` mixes query/command/mapping
      and is the class most likely to keep growing — split it when the ticket ledger lands.
- [ ] **`Modal.vue`** — add Escape-to-close, focus trap, and body scroll lock (it's the shared
      overlay primitive, so one fix pays off everywhere).
- [ ] **Duplicate QR deps** in the frontend `package.json` — `qrcode` + `qrcode.vue` +
      `@vueuse/integrations` (only `useQRCode`, which wraps `qrcode`, is used). Drop `qrcode.vue` if
      truly unused.
- [ ] **Extract `closeAsAutoClosed(Session)`** in `SessionService` — the "expired → setOpen(false),
      AUTO_CLOSE, save" logic is written twice.
- [ ] **Local-dev convenience** — a documented one-liner/script to drop & recreate the local `choir`
      DB from scratch.
