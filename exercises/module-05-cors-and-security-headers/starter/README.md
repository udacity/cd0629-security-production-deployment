# Exercise: Apply CORS and Security Header Configuration

**Estimated time:** 10–20 minutes

## The scenario

You're a backend engineer at **Harborlight**, a crowdfunding platform for
small nonprofits. Theo, who leads the frontend team, messages you Monday
morning, clearly rattled:

> "Got an email from a donor overnight — she was about to enter her card
> number on `harborlight-donate.com` (note: **not** our domain) before she
> noticed the URL looked off. Turns out it's a near-pixel-perfect clone of
> our real donation page, sitting in an iframe pointed at *our* API.
>
> I checked and... yeah, our API will currently accept a request from
> literally any origin. There's nothing stopping someone from iframing our
> real page, or standing up a clone that talks to our real backend like
> it's their own.
>
> I need our API locked to only accept calls from `app.harborlight.example`
> — that's our real SPA — and I want every security header we can
> reasonably add so a clone site can't iframe us, can't downgrade to HTTP,
> and can't get away with injecting a script into a cloned page even if
> they compromise something. Can you get this in today? I don't want a
> second email like this one."

You pull up `authdemo`. `CampaignController` and `DonationController`
already exist, and most of `SecurityConfig` — HSTS, `X-Content-Type-Options`,
`X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, and the
SPA-friendly cookie-based CSRF setup — is already wired as a worked
example. Two things are still open doors:

1. **CORS is wide open.** `corsConfigurationSource()` currently allows any
   origin — literally what let a clone site talk to the real API.
2. **The Content-Security-Policy is a placeholder** (`default-src 'self'`
   and nothing else) — no nonce, no `frame-ancestors`, nothing stopping
   the exact iframe-clone attack Theo described.

## Your tasks

1. [`SecurityConfig.java`](src/main/java/com/harborlight/authdemo/config/SecurityConfig.java) —
   fix `corsConfigurationSource()`: restrict it to `https://app.harborlight.example`,
   the actual methods the frontend calls (`GET`, `POST`), and allow
   credentials (the CSRF cookie needs to travel with cross-origin requests).
2. [`CspNonceHeaderWriter.java`](src/main/java/com/harborlight/authdemo/config/CspNonceHeaderWriter.java) —
   fill in `buildPolicy(nonce)`. The class-level comment there has the
   exact directives you need and why each one matters for Theo's incident
   specifically.

## Running it

```bash
./mvnw spring-boot:run
```

## Verifying your work

```bash
# Right now (before your fix), this "attacker" origin gets allowed:
curl -i -X OPTIONS http://localhost:8080/api/campaigns \
  -H "Origin: https://evil-lookalike.example" \
  -H "Access-Control-Request-Method: GET"
# Access-Control-Allow-Origin: * <- the bug Theo described

# After your fix, the same request should come back with NO
# Access-Control-Allow-Origin header at all (rejected), and this one
# should succeed:
curl -i -X OPTIONS http://localhost:8080/api/campaigns \
  -H "Origin: https://app.harborlight.example" \
  -H "Access-Control-Request-Method: GET"
# Access-Control-Allow-Origin: https://app.harborlight.example

# The CSP header should include a nonce that changes on every request:
curl -sD - -o /dev/null http://localhost:8080/api/campaigns | grep -i content-security-policy
```

> **Note on HSTS:** Spring Security only sends `Strict-Transport-Security`
> over HTTPS (sending it over plain HTTP is meaningless — you're already
> not on HTTPS). `curl` against `http://localhost:8080` won't show it. The
> given test simulates HTTPS with `.secure(true)`; if you want to see it
> for real, you'd need to run this behind local TLS.

### If you want to see this in an actual browser

Everything above is fully verifiable from the command line, but if you'd
like to see it the way Theo would — in DevTools — you can run this
locally and open Chrome/Firefox DevTools' **Network** tab:
- Click any request to `/api/campaigns` and check the **Response Headers**
  section for `Content-Security-Policy`, `X-Frame-Options`, etc.
- The **Console** tab will show a CSP violation warning (in red) if a page
  you're viewing tries to do something the policy blocks — that's the
  live version of what `/csp-reports` receives programmatically.
- You won't have a real `app.harborlight.example` SPA to test CORS against
  interactively without setting one up — the `curl` checks above are the
  practical way to verify that part.

Or run the test skeletons — 2 are given as worked examples, 2 are `TODO`:

```bash
./mvnw test -Dtest=SecurityHeadersTest
```

## Task List

- [ ] Restrict `corsConfigurationSource()` to Harborlight's real SPA origin
- [ ] Implement `CspNonceHeaderWriter.buildPolicy(nonce)`
- [ ] Fill in `corsPreflightRejectsUntrustedOrigin` in `SecurityHeadersTest`
- [ ] Fill in `corsPreflightAcceptsHarborlightSpaOrigin` in `SecurityHeadersTest`
- [ ] All 4 tests in `SecurityHeadersTest` pass

## Free Response

In 50–100 words: Theo asks why you're using a CSP **nonce** instead of just
adding `'unsafe-inline'` to `script-src` — wouldn't that also let
Harborlight's own inline scripts run? What would you tell him is the
security difference between those two choices, in terms of what each one
actually stops a cloned or compromised page from doing?

*(A good reflection response is between 50 and 100 words.)*
