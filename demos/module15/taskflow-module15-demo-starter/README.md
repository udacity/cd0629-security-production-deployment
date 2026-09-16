# TaskFlow API — Demo 3 SOLUTION project

This is the completed "after" state for Module 3, "Solution: Apply
Custom Authentication Configuration." It picks up right where the Demo
(starter) project left off: same `TaskController`, same `Task` model,
but now with a full custom authentication setup layered on top.

Unlike the Demo project, this one is built forward in one pass — no
commenting/uncommenting files needed. Each step below adds one real
piece and leaves the app in a working state before moving to the next.

## Before you record

**Not compiled in the sandbox that generated it** (no Maven Central
access there) — do a local `mvn clean install` first.

1. Open the project in your IDE.
2. Run `com.taskflow.util.PasswordHashGenerator` (right-click → Run). It
   prints an Argon2id hash for the password `password123`.
3. Copy that hash into `DemoUserDetailsService.PASSWORD_HASH`, replacing
   the placeholder string.
4. Confirm `DemoUserDetailsService.ACTIVE` is set to `true` before you
   start recording — you'll flip it to `false` live, near the end.

## How the project maps to the script

| Script beat | What's already in the code |
|---|---|
| `SecurityFilterChain` bean, lambda DSL | `SecurityConfig.filterChain(...)` |
| Protect only task creation | `.requestMatchers(HttpMethod.POST, "/api/v1/tasks").authenticated()` |
| HTTP Basic + CSRF off (curl, not a browser session) | `.httpBasic(...)`, `.csrf(csrf -> csrf.disable())` |
| Argon2id password encoder | `SecurityConfig.passwordEncoder()`, backed by `PasswordHashGenerator` |
| Custom `UserDetailsService` | `DemoUserDetailsService` — one hardcoded user, `dev1` |
| Custom `AuthenticationProvider` | `CustomAuthenticationProvider` — validates credentials, then checks `active` |
| "Prove the custom provider is running" | `log.info("Custom provider checked {}", username)` inside `authenticate()` |
| "Deactivate the account, show it fail" | Flip `DemoUserDetailsService.ACTIVE` to `false`, restart, re-run the same request |

## Commands to run on camera

Start the app:
```bash
mvn spring-boot:run
```

**Protect the endpoint, test with no credentials.**
```bash
curl -i -X POST http://localhost:8080/api/v1/tasks
```
Expect `401 Unauthorized`.

**Same request, with credentials.**
```bash
curl -i -u dev1:password123 -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Test task"}'
```
Expect `201 Created` with the task echoed back as JSON.

**Prove the custom provider fired.**
Re-run the authenticated `POST` above, then check the **app's console**
(not curl's output — curl only shows the HTTP response) for:
```
Custom provider checked dev1
```

**Deactivate the account.**
In `DemoUserDetailsService`, set:
```java
private static final boolean ACTIVE = false;
```
Restart, re-run the same authenticated `POST`. Expect `401` again — HTTP
Basic maps every authentication failure to 401, so the status code alone
won't distinguish this from a bad password. The proof is in the app
console instead:
```
Rejected dev1 — account is deactivated
```
Contrast that against the plain `Custom provider checked dev1` line from
the earlier successful login — same provider, different outcome, and
you can point at exactly which line in the code decided that.

**Reset before wrapping up.** Flip `ACTIVE` back to `true`, restart, and
re-confirm `201` so the project is left clean.

## Troubleshooting

**`NoClassDefFoundError: org/bouncycastle/crypto/params/Argon2Parameters$Builder`**
`Argon2PasswordEncoder` delegates to Bouncy Castle for the actual Argon2
hashing, and `spring-boot-starter-security` doesn't pull that in by
default. This project's `pom.xml` already includes it:
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.83</version>
</dependency>
```
If you still hit this, confirm Maven actually re-resolved dependencies
after any `pom.xml` edit (`mvn clean install`).

## Regenerating the password hash

If you want to demo with a different plaintext password, run:
```bash
mvn compile exec:java -Dexec.mainClass=com.taskflow.util.PasswordHashGenerator -Dexec.args="yourPasswordHere"
```
(or just run `PasswordHashGenerator.main()` from the IDE with a program
argument) and paste the printed hash into `DemoUserDetailsService`.

---

## Module 5 — Apply Form-Based Authentication with Remember-Me

This project now covers Module 5 too. Demo 5 uses this same project
exactly as Module 3 left it (HTTP Basic) to show the problem — a raw
browser popup, no session, credentials resent on every request. Solution
5 is what's currently in the code: form login, remember-me, a one-session
cap, and account lockout after repeated failures.

**New files:**
- `LoginAttemptService.java` — tracks failed logins per username,
  in-memory. Threshold comes from `security.lockout.max-attempts` in
  `application.yml`, not hardcoded.
- `CustomAuthenticationFailureHandler.java` — records each failure, then
  falls back to Spring's normal redirect-to-login-with-error behavior.
- `src/main/resources/static/demo-trigger.html` — a one-button page,
  served automatically by Spring Boot's static resource handling at
  `http://localhost:8080/demo-trigger.html`. Uses `fetch()` to send a
  real JSON `POST`, the same way an actual frontend would, not a raw
  HTML form (which sends form-urlencoded data the API can't parse and
  causes a page navigation). Exists purely to get the browser to issue a
  credentialed request, since typing a URL into an address bar only ever
  sends `GET`. Browsers show the same native Basic Auth popup for a
  `fetch()` call as they do for a full page load, so this is realistic,
  not a shortcut. Used throughout Demo 5's recording.

**Changed files:**
- `CustomAuthenticationProvider.java` — now also checks
  `loginAttemptService.isBlocked(username)` before checking the password,
  same pattern as the `active` check from Module 3. On success, calls
  `loginAttemptService.loginSucceeded(username)` to clear the count.
- `SecurityConfig.java` — `.httpBasic(...)` replaced with
  `.formLogin(...)`, plus `.rememberMe(...)` and
  `.sessionManagement(session -> session.maximumSessions(1))`.

### Recording flow

**Demo (the problem):**
1. `mvn spring-boot:run` on this project exactly as-is.
2. In a browser, go to a URL that triggers the protected `POST`. You'll
   get the browser's native Basic Auth popup, not a page — no branding,
   no "forgot password."
3. Log in, open a new tab, hit the endpoint again — prompted again, no
   session at all.
4. Fully quit and reopen the browser, same thing — no "remember me," no
   real logout.

**Solution (the fix), after the code changes above are in place:**
1. Restart the app. Navigate to the protected endpoint — now you get a
   real HTML login form, not a browser popup.
2. Log in with `dev1` / `password123`. Open DevTools → Application →
   Cookies, point at `JSESSIONID`.
3. Log out (or open a fresh incognito window) and log in again — this
   time check the **"Remember me on this computer"** checkbox that
   Spring's default login page renders automatically once
   `.rememberMe(...)` is configured (no custom HTML needed for this).
   After logging in, check DevTools again: alongside `JSESSIONID` you'll
   now see a second cookie, `remember-me`, containing a persistent token.
   Delete just `JSESSIONID` and reload the page — you're still logged in,
   proving the persistent cookie is doing the work.
4. Test the one-session cap: log in as `dev1` in a second, separate
   browser session (incognito), then refresh the first — it's forced
   back to login.
5. Test lockout: attempt login with the wrong password 5 times in a row
   (`security.lockout.max-attempts` in `application.yml`). The 5th
   attempt fails the same as the others visually, but check the app
   console for:
   ```
   Blocked login attempt for dev1 — too many failed attempts
   ```
   even a 6th attempt with the *correct* password now fails the same
   way — that's the proof it's genuinely locked, not just a bad-password
   coincidence.
6. **Reset before wrapping up:** restart the app (clears the in-memory
   attempt count) before the next take or before uploading.

---

## Module 7 — Apply Role-Based Authorization

Same project again. `dev1` is now one of three hardcoded users:
`dev1` (ROLE_USER), `manager1` (ROLE_MANAGER), `admin1` (ROLE_ADMIN) — all
sharing the same password (`password123`, same hash as before).

**New/changed files:**
- `Task.java` — added `owner` and `completed` fields.
- `TaskService.java` (new) — extracted business logic out of the
  controller. Hosts `completeTask(Task task)`, annotated
  `@PreAuthorize("#task.owner == authentication.name")`, and
  `completeTaskInternally(Task task)`, which calls `completeTask(...)`
  directly (`this.completeTask(...)`) to demonstrate the self-invocation
  gotcha — that internal call bypasses the Spring-managed proxy, so the
  `@PreAuthorize` check never fires.
- `TaskController.java` — `createTask` now captures the owner from
  `Authentication`. New endpoints: `DELETE /api/v1/tasks/{id}` and
  `PATCH /api/v1/tasks/{id}/complete`.
- `SecurityConfig.java` — added a `RoleHierarchy` bean (`ADMIN` implies
  `MANAGER` implies `USER`), a `MethodSecurityExpressionHandler` bean
  (**required** for `@PreAuthorize` to respect the hierarchy — URL-level
  `hasRole()` picks it up automatically, method-level does not, without
  this bean), `@EnableMethodSecurity` on the class, and a
  `hasRole("MANAGER")` rule on the `DELETE` endpoint.
- `pom.xml` — added `<maven.compiler.parameters>true</maven.compiler.parameters>`.
  Required for `#task.owner` in the `@PreAuthorize` expression to resolve
  the parameter name `task` by reflection. Without this, that expression
  fails at runtime regardless of what the code looks like.

### Recording flow

**Demo (the problem):** using the project exactly as Module 5 left it
(no roles, no ownership concept at all):
1. `mvn spring-boot:run`. Log in as `dev1`, create a task.
2. Point out the returned JSON has no concept of an owner, and there's
   nothing in `SecurityConfig` beyond a single blanket `authenticated()`
   rule — no roles anywhere.
3. Lay out what's missing: a role hierarchy, a URL-level restriction so
   only managers can delete tasks, and a method-level rule so users can
   only complete their own tasks.

**Solution (the fix), after the code changes above are in place:**
1. Log in as `dev1`, create a task — point out the response now includes
   `"owner":"dev1"`.
2. Try `DELETE /api/v1/tasks/{id}` as `dev1` → expect `403`. Log in as
   `manager1`, same request → expect `204 No Content`.
3. Log in as `admin1` (never granted `ROLE_MANAGER` directly), same
   `DELETE` → expect `204` as well — proof the role hierarchy is working,
   not just a coincidence.
4. As `dev1`, create a task, then `PATCH /api/v1/tasks/{id}/complete` on
   your own task → expect `200`.
5. As `manager1`, try to complete `dev1`'s task → expect `403`, even
   though `manager1` outranks `dev1` in the role hierarchy — ownership
   and role are two separate, independent checks here, worth saying
   explicitly on camera.
6. Demonstrate the self-invocation gotcha: temporarily change the
   controller's `completeTask` method to call
   `taskService.completeTaskInternally(task)` instead of
   `taskService.completeTask(task)`, restart, repeat step 5 — this time
   `manager1` succeeds completing `dev1`'s task, `200` instead of `403`,
   because the internal call bypassed the proxy. Revert the controller
   change immediately after showing this, don't leave the bypass in
   place.

Since Module 5 switched this project to form login, plain `-u` (HTTP
Basic) won't authenticate anymore — you need a real session cookie.
Log in via `curl` first, saving the cookie, then reuse it:

```bash
# Log in as dev1, save the session cookie
curl -i -c dev1-cookies.txt -X POST http://localhost:8080/login \
  -d "username=dev1&password=password123"

# Use that session for subsequent requests
curl -i -b dev1-cookies.txt -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" -d '{"title":"Test task"}'

curl -i -b dev1-cookies.txt -X DELETE http://localhost:8080/api/v1/tasks/1
curl -i -b dev1-cookies.txt -X PATCH http://localhost:8080/api/v1/tasks/1/complete

# Repeat for manager1 and admin1 with their own cookie jars
curl -i -c manager1-cookies.txt -X POST http://localhost:8080/login \
  -d "username=manager1&password=password123"
curl -i -b manager1-cookies.txt -X DELETE http://localhost:8080/api/v1/tasks/1
```
On camera, testing through an actual browser (or Postman, which handles
cookies automatically) will likely read more naturally than raw `curl`
cookie-jar juggling — your call which fits the recording better.

---

## Module 9 — Apply OAuth2 Resource Server with JWT

This is the biggest jump in the series: TaskFlow stops managing its own
users entirely and delegates identity to Keycloak, a real Authorization
Server. Everything built in Modules 3 and 5
(`CustomAuthenticationProvider`, `DemoUserDetailsService`,
`CustomAuthenticationFailureHandler`, `LoginAttemptService`) is now
**retired** — `@Component` commented out on each, code left in place as
a reference for what Keycloak now does instead. Module 7's role
hierarchy and method security are unchanged; they don't care where a
role came from.

**New/changed files:**
- `docker-compose.yml` (new) — runs Keycloak locally on port 8081.
- `pom.xml` — added `spring-boot-starter-oauth2-resource-server`.
- `application.yml` — added the Keycloak issuer-uri, commented out the
  now-unused lockout threshold.
- `SecurityConfig.java` — completely rewritten. `formLogin`,
  `rememberMe`, `sessionManagement(maximumSessions(1))`, and the custom
  `authenticationProvider` are gone, replaced with
  `.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`, a custom
  `JwtAuthenticationConverter` mapping Keycloak's `realm_access.roles`
  claim into `ROLE_`-prefixed authorities, and
  `SessionCreationPolicy.STATELESS`.
- **`TaskController.java` and `TaskService.java` are untouched.** Worth
  saying explicitly on camera — swapping the entire authentication
  mechanism didn't ripple into business logic at all, because
  `Authentication.getName()` still returns the username either way.

### One-time Keycloak setup (do this before recording, not on camera)

1. `docker compose up -d` from this project's root.
2. Go to `http://localhost:8081`, log in as `admin` / `admin`.
3. Create a new realm called `taskflow`.
4. Create a client:
   - Client ID: `taskflow-postman`
   - Client authentication: **off** (public client)
   - Authentication flow: **Standard flow** only (Authorization Code)
   - Valid redirect URIs: `https://oauth.pstmn.io/v1/callback` (Postman's
     official OAuth callback)
   - Advanced settings → Proof Key for Code Exchange: **S256**
     (this is the PKCE requirement from Module 8 — Password grant is
     gone in OAuth 2.1, this project uses the real flow, not a shortcut)
5. Create three realm roles: `USER`, `MANAGER`, `ADMIN` (Realm roles,
   not client roles).
6. Create three users, all with password `password123` (Users → Add
   user → Credentials tab, turn **off** "Temporary"):
   - `dev1` → realm role `USER`
   - `manager1` → realm role `MANAGER`
   - `admin1` → realm role `ADMIN`

### Getting a token in Postman

1. New request → Authorization tab → Type: **OAuth 2.0**.
2. Grant type: **Authorization Code (with PKCE)**.
3. Callback URL: `https://oauth.pstmn.io/v1/callback`.
4. Auth URL: `http://localhost:8081/realms/taskflow/protocol/openid-connect/auth`
5. Access Token URL: `http://localhost:8081/realms/taskflow/protocol/openid-connect/token`
6. Client ID: `taskflow-postman`.
7. Click **Get New Access Token** — a browser popup opens, log in as
   whichever user you're testing (`dev1`, `manager1`, or `admin1`).
8. Postman captures the token; click **Use Token** to attach it as a
   Bearer token on the request.

### Recording flow

**Demo (the problem):** this is a conceptual demo, not a broken-code
one — there's nothing to exploit, the point is showing what still lives
in *our* code that really shouldn't.
1. Open `DemoUserDetailsService.java` and `CustomAuthenticationProvider.java`
   from Module 5. Point out: we're storing password hashes ourselves,
   tracking lockouts ourselves, all of it homemade.
2. "Every one of these is a thing a real company would rather not build
   or maintain themselves. Let's hand it off to something built for
   exactly this."

**Solution (the fix):**
1. Show Keycloak running, the `taskflow` realm, the three users already
   created (do the realm/client/user creation live if you want the full
   click-through on camera, or narrate over it having already been set
   up — your call given time).
2. Walk through the new `SecurityConfig.java`: `oauth2ResourceServer`,
   the custom `JwtAuthenticationConverter`, `SessionCreationPolicy.STATELESS`.
   Point at the `realm_access.roles` mapping specifically — this is the
   one piece that's Keycloak-specific, not something `oauth2ResourceServer`
   gives you for free.
3. Get a token in Postman as `dev1`, `POST /api/v1/tasks` → expect `201`,
   point out `"owner":"dev1"` in the response — same behavior as Module 7,
   different identity source entirely.
4. As `dev1`, `DELETE /api/v1/tasks/1` → expect `403`.
5. Get a new token as `manager1`, same `DELETE` → expect `204`.
6. Get a token as `admin1` (realm role `ADMIN`, never granted `MANAGER`
   directly) → same `DELETE` → expect `204` — the role hierarchy from
   Module 7 still works, now driven by Keycloak-issued roles.
7. As `dev1`, create a task, `PATCH /api/v1/tasks/{id}/complete` on your
   own task → `200`. As `manager1`, same task → `403` — ownership check
   from Module 7 still works too, untouched.
8. Close by opening `TaskController.java` — point out it's identical to
   Module 7. "The business logic never needed to know how you proved who
   you are."

---

## Module 11 — Apply CORS and Security Header Configuration

Same project, no auth changes this time. This module adds two things
that don't care whether a request carries a session cookie or a JWT:
CORS (governs whether a browser-based frontend on a different origin can
call this API at all) and a baseline of security response headers.

**New files:**
- `cors-frontend/index.html` — a real second-origin frontend. Served
  separately from TaskFlow itself (see setup below), so a request from
  this page to the API is a genuine cross-origin request, not a
  simulation.
- `cors-frontend/attacker.html` — a page playing the role of a malicious
  site, trying to embed `demo-trigger.html` in an iframe, used to prove
  clickjacking protection is working.

**Changed files:**
- `SecurityConfig.java` — added `corsConfigurationSource()`, scoped to
  exactly `http://localhost:3000`, never a wildcard. Added `.cors(...)`
  to the filter chain. Added a full `.headers(...)` block: `frameOptions`
  (clickjacking), `contentSecurityPolicy`, `httpStrictTransportSecurity`,
  `contentTypeOptions`, `referrerPolicy`, and the three cross-origin
  isolation headers from Module 10 (`crossOriginOpenerPolicy`,
  `crossOriginEmbedderPolicy`, `crossOriginResourcePolicy`), plus
  `Permissions-Policy` via a raw `StaticHeadersWriter` (Spring's servlet
  stack has no dedicated DSL method for that one yet).

### One-time setup

The `cors-frontend` folder needs to be served on port **3000**, genuinely
separate from TaskFlow's own port 8080 — that's the whole point, it has
to be a real different origin, not just a different folder. No build
tooling needed, plain Python works:

```bash
cd cors-frontend
python3 -m http.server 3000
```

Leave that running in its own terminal alongside `mvn spring-boot:run`
and `docker compose up -d` (Keycloak isn't actually needed for this
module's CORS/header tests, since `GET /api/v1/tasks` is `permitAll()`,
but leave it running if you're recording continuously from Module 9).

### Recording flow

**Important correction, found during rehearsal:** Spring Security ships
`X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff` as
**default headers, with zero configuration** — this has been true for
over a decade. The starter project blocks the `attacker.html` iframe
correctly, even though `SecurityConfig.java` has no `.headers(...)`
block at all yet. That's not a bug in the starter, it's Spring Security
working as designed. Adjust the "before/after" framing accordingly — see
below.

**Demo (the problem):** using the project exactly as Module 9 left it,
before any of the changes above.
1. Start `cors-frontend` on port 3000 (see setup above). Open
   `http://localhost:3000` in the browser.
2. Click "Fetch Tasks from localhost:8080." Open the browser's DevTools
   console — show the CORS error blocking the request. No CORS config
   exists yet, so this fails.
3. Open `http://localhost:3000/attacker.html`. The iframe will **fail**
   to render here too — DevTools console shows `Refused to display ...
   X-Frame-Options to 'deny'`. Narrate this as: clickjacking protection
   is already on, by default, we didn't ask for it. One thing already
   covered.
4. With the app still running, open DevTools Network tab, load
   `GET /api/v1/tasks`, inspect the response headers. Point out what's
   genuinely **absent**: `Content-Security-Policy`, `Referrer-Policy`,
   `Cross-Origin-Opener-Policy`, `Cross-Origin-Embedder-Policy`,
   `Cross-Origin-Resource-Policy`, `Permissions-Policy`. These are the
   real gap.
5. Lay out what's missing: an explicit CORS allowlist, and the headers
   Spring doesn't provide automatically.

**Solution (the fix):**
1. Add `corsConfigurationSource()`, wire in `.cors(Customizer.withDefaults())`.
2. Restart, retry the fetch from `localhost:3000` — now succeeds. Open
   DevTools Network tab, show `Access-Control-Allow-Origin:
   http://localhost:3000` in the response headers.
3. Add the full `.headers(...)` block. Narrate `frameOptions` and
   `contentTypeOptions` as "already there by default, we're just making
   it explicit so it can't be silently lost later" — don't expect or
   claim a visible change from these two specifically.
4. Note `httpStrictTransportSecurity` won't appear in DevTools during
   local testing at all — that header only sends over an actual secure
   (HTTPS) connection, plain `http://localhost` never triggers it,
   before or after this line.
5. Restart, hit any endpoint, open DevTools Network tab. The real proof
   of this module's work is these six, genuinely new: `Content-Security-Policy`,
   `Referrer-Policy`, `Cross-Origin-Opener-Policy`,
   `Cross-Origin-Embedder-Policy`, `Cross-Origin-Resource-Policy`,
   `Permissions-Policy`.
6. Reload `http://localhost:3000/attacker.html` for completeness — still
   correctly blocked, same as the Demo. Nothing changed here, and that's
   fine — it was never broken.

---

## Module 13 — Apply OWASP Mitigations in a Spring Boot Application

This module covers 6 of the OWASP Top 10 items hands-on, with real
before/after code. Two items (**Broken Access Control**, **Identification
and Authentication Failures**) are **not** rebuilt here — they were
already solved in Modules 7 and 9 respectively, and this module just
points back at that work rather than re-demonstrating it. Three more
items get only a brief conceptual mention, because they have dedicated
modules of their own coming up: **secrets/Cryptographic Failures**
(Module 15, Vault), and **Logging and Monitoring Failures** (Modules 19,
21, 23).

**New files:**
- `model/Account.java`, `dto/AccountDTO.java`, `repository/AccountRepository.java`,
  `service/AccountService.java`, `controller/AccountController.java` —
  Injection + Insecure Design. A real (in-memory H2) database now exists
  specifically so Injection has a genuine attack surface.
- `controller/ReportController.java` — XSS. A server-rendered HTML report,
  the one place in TaskFlow where HTML actually gets rendered.
- `controller/PartnerController.java` — SSRF. Simulates fetching data
  from a partner service on the user's behalf.
- `src/main/resources/data.sql` — seed accounts, including a "victim"
  account with a distinctive balance/risk score to prove data theft via
  injection.

**Changed files:**
- `pom.xml` — added `spring-boot-starter-data-jpa`, `h2` (runtime),
  `spring-boot-starter-actuator`, `org.owasp.encoder:encoder`, and (Solution
  only) the `dependency-check-maven` plugin.
- `application.yml` — H2 datasource config, and `management.endpoints.web.exposure.include`
  (Security Misconfiguration: `*` in the starter, `health` only in the Solution).

### Before you record: the dependency-check plugin

**Do not run this for the first time on camera.** `mvn verify` (which
triggers the `dependency-check-maven` plugin) downloads the entire NVD
vulnerability database on its first run — this can take anywhere from
several minutes to well over an hour depending on rate limits, especially
without an API key configured. Run it once, in full, well before
recording:
```bash
mvn verify
```
Subsequent runs are fast (the database is cached locally). Optional but
recommended: get a free NVD API key (https://nvd.nist.gov/developers/request-an-api-key)
and configure it via the plugin's `nvdApiKey` configuration option —
without one, the analyzer is heavily rate-limited even after the first run.

### One more thing to check before recording: the H2 console

`spring.h2.console.enabled: true` is on for convenience, but it's not
part of the scripted recording flow below — only the API endpoints are.
Worth knowing if you go poke at it out of curiosity: H2's console UI
uses an internal frame, which may collide with Module 11's
`X-Frame-Options: DENY`. If `/h2-console` renders blank or broken,
that's almost certainly why — not a Module 13 bug. Not worth fixing for
this demo since it's outside the recorded flow, but don't let it throw
you off if you go looking at it live.

### Recording flow

**Demo (the problem):** using the project exactly as this module's
starter state — H2/JPA and the new endpoints exist, but every mitigation
is still unapplied.

1. **Broken Access Control / Auth Failures — callback only.** Briefly
   reopen `TaskService.java` (Module 7's `@PreAuthorize` ownership check)
   and `SecurityConfig.java`'s `oauth2ResourceServer` block (Module 9).
   No new code — just point at what's already there.
2. **Injection + Insecure Design.** `GET /api/v1/accounts/search?name=dev1`
   → returns one account, including `internalRiskScore` (already a
   problem — that field should never be visible). Now `GET
   /api/v1/accounts/search?name=dev1' OR '1'='1` → returns **all three**
   accounts, including `admin1`'s $250,000 balance and risk score.
   One crafted search string, every account's private data.
3. **Security Misconfiguration.** `GET /actuator/env` → full environment
   variables, wide open, zero authentication.
4. **XSS.** Create a task with title `<script>alert('xss')</script>` via
   `POST /api/v1/tasks`. Load `GET /api/v1/tasks/report` in a browser,
   View Page Source — the raw `<script>` tag is sitting in the HTML,
   live.
5. **SSRF.** `GET /api/v1/partners/fetch?url=http://169.254.169.254/latest/meta-data/`
   (the classic cloud metadata target) — the server happily fetches
   whatever URL it's given, no validation at all.
6. **Vulnerable Components.** Open `pom.xml` — no `dependency-check-maven`
   plugin exists yet. Nothing is scanning for known CVEs in any dependency.

**Solution (the fix):**
1. Add the safe `findByNameSafe` query to `AccountRepository`, wire it
   through `AccountService.searchByNameSafe`, mapping to `AccountDTO`.
   Update `AccountController` to use it. Re-test the injection payload —
   `name=dev1' OR '1'='1` now returns **zero** accounts (no account is
   literally named that whole string), and a normal search for `dev1`
   returns only `{id, name, balance}` — no `internalRiskScore` field
   exists on the response at all.
2. Restrict `management.endpoints.web.exposure.include` to `health`.
   Restart, retry `GET /actuator/env` → `404`. Retry `GET /actuator/health`
   → still works, that one's meant to be public.
3. Add `Encode.forHtml(...)` around the task title in `ReportController`.
   Restart, reload `/api/v1/tasks/report`, View Page Source — the
   `<script>` tag is now literal, escaped text (`&lt;script&gt;`), not
   live markup.
4. Add the allowlist check to `PartnerController`. Retry the metadata
   URL → `400 Bad Request`, host not on the allowlist. Try a URL on the
   allowlist (`trusted-partner.com`) — request proceeds normally (it'll
   fail to actually connect since that domain doesn't really exist for
   this demo, but the point is it gets *past* the allowlist check, not
   that the fetch succeeds).
5. Add the `dependency-check-maven` plugin to `pom.xml` (already run
   once before recording, per the warning above). Run `mvn verify` on
   camera — since the database is already cached, this should complete
   in well under a minute. Show the generated report (`target/dependency-check-report.html`)
   or the console warning if anything's flagged.
6. Close by tying it together: six real vulnerabilities, six real fixes,
   two more already handled in earlier modules, and the remaining OWASP
   items have dedicated modules of their own still coming.

