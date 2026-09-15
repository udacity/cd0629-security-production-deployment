# Exercise: Apply Form-Based Authentication with Remember-Me

**Estimated time:** 10–20 minutes

## The scenario

You're a backend engineer at **Encore Tickets**, a live-events ticketing
startup. Last night was the on-sale for a sold-out arena show, and this
morning your support lead, Devon, is waiting for you in Slack:

> "Two things from last night, both in the `authdemo` service:
>
> 1. Support got flooded with 'I keep getting logged out mid-checkout'
>    tickets. People are re-entering their card details three times because
>    their session doesn't survive a tab switch. We need 'remember me' on
>    the login form — backed by a real token store, not just a long session
>    cookie, since infra wants to be able to revoke a stolen device's token
>    without killing everyone else's session.
>
> 2. Our WAF logs show something like credential stuffing against `/login`
>    from a handful of IPs — dozens of attempts against the same handful of
>    usernames. We don't have a WAF rule for this yet, so in the meantime
>    I want the app itself to lock an account after 5 failed attempts.
>
> Oh — and separate thing, not urgent but annoying: when a venue admin logs
> in they land on the customer dashboard like everyone else and have to
> click over to the admin console manually. Can you make it redirect them
> straight to `/admin` while you're in there?
>
> No rush on the WAF rule, but the lockout and the redirect are both quick,
> right? Ship whenever."

You pull up `authdemo`. The login page, the `User` entity (already has
`failedAttempts` and `accountLocked` columns), and the `/admin` and
`/dashboard` pages all exist. `SecurityConfig` already has the authorization
rules, the Argon2 password encoder, and the remember-me token store wired up
as a worked example — what's missing is the actual behavior Devon asked for.

## Your tasks

Two files, two `TODO`s:

- [`RoleBasedAuthenticationSuccessHandler.java`](src/main/java/com/encoretickets/authdemo/security/RoleBasedAuthenticationSuccessHandler.java) —
  redirect `ROLE_ADMIN` users to `/admin`, everyone else to `/dashboard`.
- [`AccountLockoutAuthenticationFailureHandler.java`](src/main/java/com/encoretickets/authdemo/security/AccountLockoutAuthenticationFailureHandler.java) —
  on a failed login, look the user up, increment their failed-attempt
  counter, and lock the account once it hits 5.

Then wire both into `formLogin()` in
[`SecurityConfig.java`](src/main/java/com/encoretickets/authdemo/config/SecurityConfig.java)
(one small `TODO` there — `.successHandler(...)` / `.failureHandler(...)`).
Remember-me itself is already fully configured; you don't need to touch it.

## Seed data

`data.sql` seeds three users with real Argon2id password hashes:

| username | password              | role  | notes |
|----------|-----------------------|-------|-------|
| alice    | `BoxOffice!2025`       | ADMIN | venue manager |
| bob      | `FrontRow!2025`        | USER  | use for the redirect / remember-me checks |
| carol    | `BackstagePass!2025`   | USER  | dedicated to the lockout check, so you can hammer her account without breaking bob's tests |

## Running it

```bash
./mvnw spring-boot:run
```

Then open `http://localhost:8080/login` in a browser — the form, CSRF token,
and remember-me checkbox are already there.

## Verifying your work

Right now, logging in as anyone redirects to `/dashboard` (even alice), and
failed logins just bounce back to `/login?error` with no lockout — that's
the "not done yet" state. Once both TODOs are done:

- Log in as `alice` / `BoxOffice!2025` → should land on `/admin`.
- Log in as `bob` / `FrontRow!2025` → should land on `/dashboard`.
- Check "remember me" while logging in as `bob` → response should set a
  `remember-me` cookie.
- Submit `carol` with the wrong password 5 times in a row, then submit her
  **correct** password on the 6th try → should still bounce to
  `/login?error`, because the account is now locked.

Or just run the (currently empty) test skeletons and fill them in as you go:

```bash
./mvnw test -Dtest=SecurityConfigTest
```

You're done when all 5 tests in `SecurityConfigTest` pass.
