# Exercise: Apply Custom Authentication Configuration

**Estimated time:** 10–20 minutes

## The scenario

It's Thursday afternoon at **Udabank**, a small fintech startup building
invoicing software for freelancers. You joined the backend team two weeks ago.

Your CTO, Priya, drops into your DMs:

> "Hey — compliance review is tomorrow morning and I just realized `/app/**`
> has zero authentication. Anyone who knows the URL can hit the dashboard.
> I need a fix shipped today, not a perfect one. Here's what I need:
>
> 1. Keep `/public/**` open — that's the marketing/health-check stuff, fine as is.
> 2. Lock down everything under `/app/**` so only logged-in users can reach it.
> 3. We already have a `users` table with an `alice` (admin) and `bob` (regular
>    user) row — wire authentication to read from there, not some hardcoded list.
> 4. Security wants passwords hashed with **Argon2id** — it's what OWASP
>    recommends now, and BCrypt alone won't pass the audit.
> 5. Frontend hasn't built the real login page yet, so don't wire up form
>    login. Marcus on QA needs to be able to test this from curl/Postman
>    this afternoon — just get HTTP Basic working as a stopgap.
>
> Ship it, then ping me — I want to show this to the auditor tomorrow."

You pull up the `authdemo` service. The `User` JPA entity, `UserRepository`,
and REST controllers for `/public/**` and `/app/**` already exist. What's
missing is the security wiring itself: a `DbUserDetailsService` that actually
reads from the database, and a `SecurityFilterChain` that enforces Priya's
rules using the Spring Security 7 lambda-only DSL.

## Your tasks

Open these two files — both have `TODO` markers. The `/public/**` rule,
HTTP Basic, and the disabled form login are already wired up in
`SecurityConfig` as a worked example — you're extending that pattern, not
starting from a blank file.

- [`DbUserDetailsService.java`](src/main/java/com/udabank/authdemo/user/DbUserDetailsService.java) —
  implement `loadUserByUsername` to look the user up via `UserRepository` and
  map it to a Spring Security `UserDetails` (role → `ROLE_` authority,
  `enabled` → `disabled`).
- [`SecurityConfig.java`](src/main/java/com/udabank/authdemo/config/SecurityConfig.java) —
  two things:
  1. A `PasswordEncoder` bean using `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`.
  2. Two more `authorizeHttpRequests` rules below the given `/public/**` example:
     `/app/**` → `authenticated()`, and `anyRequest()` → `denyAll()` (fail closed).

That covers requirements (a)–(d) from Priya's message. `PathPatternRequestMatcher`
and the lambda-only DSL are already used in the example rule — just mirror it.

## Seed data

`data.sql` already seeds two users with real Argon2id password hashes:

| username | password         | role  |
|----------|------------------|-------|
| alice    | `VaultAdmin!2025` | ADMIN |
| bob      | `QaTester!2025`   | USER  |

## Running it

```bash
./mvnw spring-boot:run
```

## Verifying your work

Before you touch any code, run `./mvnw spring-boot:run` and try the two
endpoints: `/public/health` already returns **200** (that's the given
example rule), but `/app/dashboard` returns **401** no matter what
credentials you pass — the `PasswordEncoder` bean doesn't exist yet, so
`DbUserDetailsService` can't authenticate anyone. Once you finish both
TODOs, all four checks below should pass:

```bash
# (a) Public endpoint — should be 200 with no credentials
curl -i http://localhost:8080/public/health

# (b) App endpoint, anonymous — should be 401
curl -i http://localhost:8080/app/dashboard

# (c) + (d) App endpoint with valid DB-backed credentials over HTTP Basic — should be 200
curl -i -u alice:VaultAdmin!2025 http://localhost:8080/app/dashboard

# Wrong password — should be 401
curl -i -u alice:wrong-password http://localhost:8080/app/dashboard
```

Or run the (currently empty) test skeletons and fill them in as you go:

```bash
./mvnw test -Dtest=SecurityConfigTest
```

You're done when all four `curl` checks above behave as described and the
tests in `SecurityConfigTest` pass.
