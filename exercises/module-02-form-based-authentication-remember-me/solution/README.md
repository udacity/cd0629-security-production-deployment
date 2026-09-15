# Solution: Apply Form-Based Authentication with Remember-Me

**Scenario recap:** Encore Tickets needed remember-me (backed by a real
token store, not just a long session), account lockout after 5 failed
logins to blunt a credential-stuffing pattern support was seeing, and
role-based redirects so venue admins land on `/admin` instead of the
customer dashboard. See [`../starter/README.md`](../starter/README.md) for
Devon's full message and the task breakdown.

## What changed

- [`RoleBasedAuthenticationSuccessHandler.java`](src/main/java/com/encoretickets/authdemo/security/RoleBasedAuthenticationSuccessHandler.java) —
  checks `authentication.getAuthorities()` for `ROLE_ADMIN` and redirects to
  `/admin`; everyone else goes to `/dashboard`.
- [`AccountLockoutAuthenticationFailureHandler.java`](src/main/java/com/encoretickets/authdemo/security/AccountLockoutAuthenticationFailureHandler.java) —
  reads the submitted `username`, loads the `User`, calls
  `incrementFailedAttempts()`, and calls `lock()` once the count reaches 5,
  then saves. Still redirects to `/login?error` either way, so a would-be
  attacker can't tell a locked account from a wrong password.
- [`SecurityConfig.java`](src/main/java/com/encoretickets/authdemo/config/SecurityConfig.java) —
  both handlers wired into `formLogin()` via `.successHandler(...)` /
  `.failureHandler(...)`.

## Solution walkthrough: inspecting persistent_logins entries

Devon's likely follow-up question: "how do we know remember-me is actually
persisting a revocable token, and not just setting a long-lived cookie the
server can't invalidate?"

[`PersistentLoginsController.java`](src/main/java/com/encoretickets/authdemo/web/PersistentLoginsController.java)
is an ADMIN-only debug endpoint (`GET /admin/persistent-logins`) that runs
a plain `SELECT` against the `persistent_logins` table `JdbcTokenRepositoryImpl`
auto-creates. Here's a real transcript from running the app locally:

```bash
# 1. bob logs in with "remember me" checked
curl -c cookies.txt http://localhost:8080/login -o login.html
CSRF=$(grep -o '_csrf" value="[^"]*"' login.html | sed 's/.*value="\(.*\)"/\1/')
curl -b cookies.txt -c cookies.txt -i -X POST http://localhost:8080/login \
  --data-urlencode "_csrf=$CSRF" \
  --data-urlencode "username=bob" \
  --data-urlencode "password=FrontRow2025" \
  --data-urlencode "remember-me=true"
```
```
HTTP/1.1 302
Set-Cookie: remember-me=N3NWJTJGMEhiVjBZQ0hNMTZNRDNzakRnJTNEJTNEOjNWSjZlcEZ1QzFtdE1FVk9xcjMxaUElM0QlM0Q; Max-Age=1209600; Path=/; HttpOnly
Location: http://localhost:8080/dashboard
```

```bash
# 2. alice (admin) logs in separately, then checks the token store
curl -b alice-cookies.txt http://localhost:8080/admin/persistent-logins
```
```json
[{"USERNAME":"bob","SERIES":"7sV/0HbV0YCHM16MD3sjDg==","LAST_USED":"2026-07-02T09:31:42.920Z"}]
```

Two things worth pointing out to Devon from that output:

- **The cookie value and the `SERIES` column are not the same string.**
  The cookie encodes a `series:token` pair; only the `series` half is
  stored in plaintext in the DB (per-login token itself is hashed). That's
  what makes this safer than a raw long-lived session: if this row is
  deleted, that specific device's cookie stops working on its next use,
  without touching any other device or session.
- **This is exactly the query you'd run to build a "log out this device"
  feature** — `DELETE FROM persistent_logins WHERE series = ?` — which is
  probably Devon's next ask once support starts getting "how do I sign out
  my old phone" requests.

## Verifying the solution

```bash
./mvnw test -Dtest=SecurityConfigTest
# 5 tests, all green
```

Manually:

```bash
./mvnw spring-boot:run
```

- `alice` / `BoxOffice!2025` → redirects to `/admin`
- `bob` / `FrontRow2025` → redirects to `/dashboard`
- `bob` with "remember me" checked → `Set-Cookie: remember-me=...`
- `carol` / wrong password × 5, then her correct password → still
  `/login?error` (locked); confirm in `admin/persistent-logins`-style
  fashion by checking `carol`'s `account_locked` column, e.g. via a JPA
  query or the H2 in-memory console if you enable it locally.

## Common pitfalls

- **Redirecting inside a `AuthenticationFailureHandler` before checking
  whether the user exists.** If you call `userRepository.findByUsername(...)`
  and the username doesn't exist at all, `.ifPresent(...)` silently no-ops —
  don't let a `NoSuchElementException` leak a 500 that would tell an
  attacker "that username exists, the password was just wrong."
- **Forgetting `failureHandler` still needs to redirect to `/login?error`
  even when the account gets locked.** Returning a different response for
  "wrong password" vs. "locked" is exactly the kind of oracle that helps
  an attacker enumerate valid usernames — same response either way, on
  purpose.
- **Mixing up `.rememberMe(...)`'s `tokenValiditySeconds` with the session
  timeout.** They're independent: the session controls how long you stay
  logged in *without* the cookie; the remember-me token controls how long
  the cookie can silently re-establish a session after that.
