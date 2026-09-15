# Solution: Apply OAuth2 Resource Server with JWT

**Scenario recap:** Udabank's Partner API validated that a bearer token was
present and genuinely signed, but never checked what that token actually
granted — so a read-only partner token could still create invoices. See
[`../starter/README.md`](../starter/README.md) for Yusuf's full report and
the task breakdown.

## What changed

[`SecurityConfig.java`](src/main/java/com/udabank/authdemo/config/SecurityConfig.java):

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("entitlements");
    authoritiesConverter.setAuthorityPrefix("SCOPE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
}
```

[`InvoiceService.java`](src/main/java/com/udabank/authdemo/invoice/InvoiceService.java):
`@PreAuthorize("hasAuthority('SCOPE_read')")` on `listInvoices()` and
`getInvoice(id)`; `@PreAuthorize("hasAuthority('SCOPE_write')")` on
`createInvoice(...)`.

## Solution walkthrough: inspecting JWT claims

Yusuf's likely follow-up: "how do I check what's actually inside a
partner's token without asking them to paste their secret into Slack?"

A JWT's header and payload are just base64url-encoded JSON — no key needed
to *read* them, only to verify the signature is genuine. Here's the
`read`+`write` token this exercise's tests mint, decoded locally (Python,
but any base64url-JSON decode works, including pasting the token into
jwt.io — the "Invalid Signature" warning jwt.io shows is expected and
harmless, since jwt.io doesn't have Udabank's private key either):

```python
import base64, json
header_b64, payload_b64, signature_b64 = token.split(".")
def decode(segment):
    return json.loads(base64.urlsafe_b64decode(segment + "=" * (-len(segment) % 4)))
print(decode(payload_b64))
```

```json
{
  "sub": "partner-quickbooks-sync",
  "iss": "https://auth.udabank.example",
  "entitlements": ["read", "write"],
  "iat": 1782987229,
  "exp": 1782987529
}
```

Two things worth pointing out to Yusuf from that output:

- **The `entitlements` claim is exactly what `jwtAuthenticationConverter()`
  reads** — if a partner reports "my read-only integration is getting 403
  on GET requests too," decoding their actual token like this is the
  fastest way to check whether their auth service issued the claim you
  expect, before you go digging through Spring config.
- **`exp` is a Unix timestamp, not a duration.** A token minted with
  `expirationTime(now + 300s)` isn't "valid for 5 minutes from whenever
  someone reads this" — it's dead at that exact instant. A partner
  integration that mints a token once and reuses it for an hour will start
  getting 401s partway through, which looks like an authorization bug but
  is really just an expired token.

## Verifying the solution

```bash
./mvnw test -Dtest=InvoiceAuthorizationTest
# 5 tests, all green
```

```bash
./mvnw spring-boot:run
```

```bash
# read-only token
curl -H "Authorization: Bearer $READ_TOKEN" http://localhost:8080/api/invoices
# 200

curl -X POST -H "Authorization: Bearer $READ_TOKEN" -H "Content-Type: application/json" \
  -d '{"customerName":"Test","amountCents":100}' http://localhost:8080/api/invoices
# 403 — Yusuf's bug, now fixed

# read+write token
curl -X POST -H "Authorization: Bearer $READWRITE_TOKEN" -H "Content-Type: application/json" \
  -d '{"customerName":"Marlowe Studio","amountCents":75000}' http://localhost:8080/api/invoices
# 201 {"id":3,"customerName":"Marlowe Studio","amountCents":75000,"status":"DRAFT"}
```

## Common pitfalls

- **Forgetting the `entitlements` claim name is non-standard for this
  project.** If you leave `jwtAuthenticationConverter()` at its default
  (unmodified `new JwtAuthenticationConverter()`), Spring Security looks
  for `scope`/`scp` — which Udabank's tokens don't have — so every token
  ends up with zero authorities and every `@PreAuthorize` check fails,
  regardless of what the token actually grants. If everything 403s no
  matter what token you send, check the converter before you suspect the
  `@PreAuthorize` expressions.
- **CSRF, again.** Same pitfall as the role-based authorization module:
  this is a stateless bearer-token API, so CSRF protection is disabled.
  If you're adapting this pattern for an API that *also* serves a browser
  client via cookies, don't copy that line blindly — CSRF protection is
  about protecting cookie-authenticated requests, and a mixed API might
  still need it for the cookie-authenticated paths.
