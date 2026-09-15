# Exercise: Apply OAuth2 Resource Server with JWT

**Estimated time:** 10–20 minutes

## The scenario

You're a backend engineer at **Udabank**. The company just launched a
Partner API so accounting-integration partners (think: QuickBooks-style
sync tools) can pull a customer's invoice data, and a small number of
vetted partners can create invoices on a customer's behalf. Everything
authenticates with a JWT bearer token issued by Udabank's auth service —
no passwords, no cookies, just "present a valid token on every request."

Yusuf, who owns the partner integrations platform, pings you from the
sandbox environment:

> "Found something during onboarding testing that's making me nervous. One
> of our sandbox partner credentials is supposed to be **read-only** —
> that's literally what they asked for, they just sync invoice data into
> their own dashboard. I gave their test token a spin against `POST
> /api/invoices` just to confirm it'd get rejected, and... it created an
> invoice. A read-only integration can write data.
>
> I checked `InvoiceService` and yeah — right now the API only checks
> 'is there a valid, signed token at all.' It doesn't look at what that
> token actually grants. Every partner token can do everything, regardless
> of what scope they were issued.
>
> Before we let any partner past sandbox, I need `GET` endpoints locked to
> a read grant and `POST` locked to a write grant, enforced from the
> token itself. Can you get this done before Friday's partner review?"

You pull up `authdemo`. `InvoiceController` and the in-memory
`InvoiceService` already exist, and `SecurityConfig` already validates
that incoming tokens are genuinely signed by Udabank's auth service (more
on that below) — what's missing is checking what a valid token is actually
*allowed* to do.

## A deliberate setup choice, so you're not confused by it

The original ask mentioned validating against a live Keycloak instance or
hand-copied jwt.io tokens. This exercise skips both — there's a local
RSA keypair already in the project (`src/main/resources/keys/public_key.pem`
for verification, `src/test/resources/keys/private_key.pem` for minting
test tokens) standing in for Udabank's real auth service. Same Spring
Security DSL, same JWT validation, zero external services or Docker
required. `TestJwtIssuer` (in the test sources) mints real, validly-signed
tokens with whatever claims you want.

## Your tasks

Two files, two `TODO`s:

1. [`SecurityConfig.java`](src/main/java/com/udabank/authdemo/config/SecurityConfig.java) —
   implement the `jwtAuthenticationConverter()` bean. Udabank's partner
   tokens carry grants in a claim called `entitlements` (a JSON array like
   `["read","write"]`) instead of the OAuth2-standard `scope` claim — a
   naming quirk from before anyone standardized on that convention. You
   need to point Spring Security's converter at that claim name and give
   the resulting authorities a `SCOPE_` prefix.
2. [`InvoiceService.java`](src/main/java/com/udabank/authdemo/invoice/InvoiceService.java) —
   add `@PreAuthorize` to `listInvoices()` and `getInvoice(id)` requiring
   `SCOPE_read`, and to `createInvoice(...)` requiring `SCOPE_write`.

Do task 1 first — until the converter reads `entitlements` correctly, no
token will ever have any authorities, so task 2's checks won't have
anything real to test against.

## Running it

```bash
./mvnw spring-boot:run
```

There's no login form here — every request needs an
`Authorization: Bearer <token>` header. Use `TestJwtIssuer.issueToken(subject, "read")`
(or `"read", "write"`) from a test, or mint one yourself with any JWT
library and the test private key.

## Verifying your work

Right now (before your TODOs), Yusuf's bug is fully reproducible: a
token with only `read` in its `entitlements` claim can still successfully
`POST /api/invoices`, because nothing checks the claim at all yet. Once
both TODOs are done:

- `GET /api/invoices` with no token → 401
- `GET /api/invoices` with a `read`-only token → 200
- `POST /api/invoices` with a `read`-only token → 403 (this is Yusuf's bug, fixed)
- `POST /api/invoices` with a `read`+`write` token → 201

Or run the test skeletons — 3 are given as worked examples, 2 (the ones
that actually prove the bug is fixed) are `TODO`:

```bash
./mvnw test -Dtest=InvoiceAuthorizationTest
```

## Task List

- [ ] Implement `jwtAuthenticationConverter()` in `SecurityConfig`
- [ ] Add `@PreAuthorize("hasAuthority('SCOPE_read')")` to `listInvoices()` and `getInvoice(id)`
- [ ] Add `@PreAuthorize("hasAuthority('SCOPE_write')")` to `createInvoice(...)`
- [ ] Fill in `readOnlyScopeTokenCannotCreateInvoice` in `InvoiceAuthorizationTest`
- [ ] Fill in `writeOnlyScopeTokenCannotListInvoices` in `InvoiceAuthorizationTest`
- [ ] All 5 tests in `InvoiceAuthorizationTest` pass