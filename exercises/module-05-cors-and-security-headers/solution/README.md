# Solution: Apply CORS and Security Header Configuration

**Scenario recap:** A lookalike domain iframed Harborlight's real donation
page to phish a donor, and Harborlight's API would accept requests from
any origin. See [`../starter/README.md`](../starter/README.md) for Theo's
full report and the task breakdown.

## What changed

[`SecurityConfig.java`](src/main/java/com/harborlight/authdemo/config/SecurityConfig.java):

```java
configuration.setAllowedOrigins(List.of(SPA_ORIGIN)); // https://app.harborlight.example
configuration.setAllowedMethods(List.of("GET", "POST"));
configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
configuration.setAllowCredentials(true);
```

[`CspNonceHeaderWriter.java`](src/main/java/com/harborlight/authdemo/config/CspNonceHeaderWriter.java):

```java
String.join("; ",
        "default-src 'self'",
        "script-src 'self' 'nonce-" + nonce + "'",
        "frame-ancestors 'none'",
        "report-uri /csp-reports");
```

`frame-ancestors 'none'` is the direct fix for Theo's incident — it's the
modern CSP replacement for `X-Frame-Options` and stops *any* site,
including the clone, from iframing this page.

## Solution walkthrough: reading CSP violation reports

Theo's likely follow-up: "how would we even know if this happens again?"

[`CspReportController.java`](src/main/java/com/harborlight/authdemo/config/CspReportController.java)
is what `report-uri /csp-reports` in the policy points browsers at — every
browser that blocks something under this policy POSTs a report there
automatically, no frontend code required. Here's a real one, captured by
POSTing what a browser actually sends for a blocked inline script (legacy
`report-uri` format; newer browsers may also use the Reporting API's
`application/reports+json` instead):

```bash
curl -i -X POST http://localhost:8080/csp-reports \
  -H "Content-Type: application/csp-report" \
  -d '{"csp-report":{"document-uri":"https://app.harborlight.example/donate", ...}}'
```
```
HTTP/1.1 204
```
```
CSP violation report received: {"csp-report":{"document-uri":"https://app.harborlight.example/donate","referrer":"","violated-directive":"script-src","effective-directive":"script-src","original-policy":"default-src 'self'; script-src 'self' 'nonce-abc123'; frame-ancestors 'none'; report-uri /csp-reports","blocked-uri":"inline","status-code":200}}
```

The field worth watching in production: `blocked-uri`. `"inline"` means an
inline `<script>` without a valid nonce tried to run — that's either a
real injection attempt, or (more often, in practice) a legitimate script
your own team added without wiring up the nonce. A `blocked-uri` pointing
at some *other* domain entirely is the more alarming case — that's
consistent with an actual injected/malicious script trying to load
something external.

## Verifying the solution

```bash
./mvnw test -Dtest=SecurityHeadersTest
# 4 tests, all green
```

```bash
./mvnw spring-boot:run
```

```bash
curl -i -X OPTIONS http://localhost:8080/api/campaigns \
  -H "Origin: https://evil-lookalike.example" -H "Access-Control-Request-Method: GET"
# HTTP/1.1 403 — no Access-Control-Allow-Origin header

curl -i -X OPTIONS http://localhost:8080/api/campaigns \
  -H "Origin: https://app.harborlight.example" -H "Access-Control-Request-Method: GET"
# Access-Control-Allow-Origin: https://app.harborlight.example
# Access-Control-Allow-Credentials: true

curl -sD - -o /dev/null http://localhost:8080/api/campaigns | grep -i content-security-policy
# Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-<different every time>'; frame-ancestors 'none'; report-uri /csp-reports
```

## Free Response — one answer

`'unsafe-inline'` trusts **every** inline script on the page, with no way
to tell "ours" from "injected" — if a clone site (or a compromised
dependency) manages to inject a `<script>` tag, `'unsafe-inline'` lets it
run exactly as happily as Harborlight's own code. A nonce is generated
fresh per request and only scripts tagged with *that exact value* are
trusted; an attacker injecting a script has no way to know or predict the
current nonce, so their script gets blocked while Harborlight's own
(correctly-tagged) scripts still run. Same convenience, real isolation.

## Common pitfalls (bugs I actually hit building this)

- **`.permissionsPolicy(...)` silently breaks the fluent `headers()`
  chain.** Unlike `.frameOptions(...)`, `.referrerPolicy(...)`, etc. — all
  of which return `HeadersConfigurer` so you can keep chaining —
  `.permissionsPolicy(...)` returns a nested `PermissionsPolicyConfig`
  instead, so anything chained after it won't compile. There's a
  `.permissionsPolicyHeader(...)` alias that returns `HeadersConfigurer`
  properly — use that one if you're chaining.
- **HSTS is a no-op over plain HTTP, by design.** If you `curl` this app
  locally over `http://` and don't see `Strict-Transport-Security`, that's
  not a bug — Spring Security only emits it for HTTPS requests, since
  telling a browser "always use HTTPS" over a channel that isn't HTTPS
  doesn't mean anything. Test it with MockMvc's `.secure(true)`, not a
  plain local `curl`.
- **Browsers don't send CSP reports as `application/json`.** They use
  `application/csp-report` (legacy `report-uri`) or
  `application/reports+json` (newer Reporting API) — neither is a media
  type Spring's Jackson converter recognizes by default, so a
  `@RequestBody` DTO parameter gets a `415 Unsupported Media Type` instead
  of your handler ever running. Accepting the body as a raw `String` (see
  `CspReportController`) sidesteps the content-type matching entirely,
  which is fine for something you're just logging.
