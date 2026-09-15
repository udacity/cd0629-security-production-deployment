# Exercise: Apply Role-Based Authorization

**Estimated time:** 10–20 minutes

## The scenario

You're a backend engineer at **Inkwell**, a proposal-and-contract SaaS for
freelancers and small agencies. Wednesday morning, a message from Jules,
who runs support:

> "Got a weird one from a customer yesterday. She was looking at her own
> contract at `/api/documents/14`, changed it to `/api/documents/15` just
> to see what would happen, and... it loaded. Somebody else's signed
> contract. Full title, everything.
>
> I checked `DocumentService` and yeah — `view()` and `delete()` don't
> check who's asking at all. Any logged-in user can read or delete *any*
> document by guessing IDs. `/admin/**` being locked to admins doesn't
> help here, because this isn't an admin route — it's every customer's
> regular document view.
>
> I need this fixed today: a customer should only see their own documents
> (support can see anyone's, we need that for troubleshooting), and only
> support/admin should be able to delete anything — I don't want a
> customer's account compromise turning into a customer's contracts
> vanishing too. Ping me when it's live so I can tell her it's fixed."

You pull up `authdemo`. The `Document` entity, `DocumentController`, and a
`DocumentSecurity` helper bean (it can check "does this user own this
document?") already exist. `SecurityConfig` already has `/admin/**` locked
down and `@EnableMethodSecurity` turned on — what's missing is the
method-level authorization Jules is describing.

## Your tasks

One file, two `TODO`s, both in
[`DocumentService.java`](src/main/java/com/inkwell/authdemo/document/DocumentService.java):

1. `view(id)` — add `@PreAuthorize` so it only succeeds for an ADMIN, or
   the document's owner.
2. `delete(id)` — add `@PreAuthorize` so it only succeeds for an ADMIN
   (owning the document isn't enough for delete).

The comment above each method gives you the SpEL building blocks
(`hasRole(...)`, the `@documentSecurity.isOwner(...)` bean reference, and
how to access the method parameter and the current `Authentication`) —
you compose the actual expression.

## Seed data

`data.sql` seeds three users and two documents:

| username | password             | role  | owns |
|----------|----------------------|-------|------|
| alice    | `SupportDesk!2025`    | ADMIN | — |
| bob      | `MyContracts!2025`    | USER  | document 1 |
| carol    | `AgencyOwner!2025`    | USER  | document 2 |

## Running it

```bash
./mvnw spring-boot:run
```

## Verifying your work

Right now, Jules's bug is still live: any authenticated user can view or
delete any document, regardless of ownership. Once both TODOs are done:

```bash
# bob viewing his own document — should be 200
curl -u bob:MyContracts!2025 http://localhost:8080/api/documents/1

# bob viewing carol's document — should be 403 (this is Jules's exact bug)
curl -i -u bob:MyContracts!2025 http://localhost:8080/api/documents/2

# alice (admin) viewing carol's document — should be 200, admins can see anything
curl -u alice:SupportDesk!2025 http://localhost:8080/api/documents/2

# bob deleting his OWN document — should be 403, owner isn't enough to delete
curl -i -u bob:MyContracts!2025 -X DELETE http://localhost:8080/api/documents/1
```

Or run the test skeletons — 3 are already given as worked examples, 2
(the ones that actually prove Jules's bug is fixed) are `TODO`:

```bash
./mvnw test -Dtest=DocumentAuthorizationTest
```

## Task List

- [ ] Add `@PreAuthorize` to `DocumentService.view(id)`
- [ ] Add `@PreAuthorize` to `DocumentService.delete(id)`
- [ ] Fill in `nonOwnerNonAdminCannotViewSomeoneElsesDocument` in `DocumentAuthorizationTest`
- [ ] Fill in `nonAdminCannotDeleteEvenTheirOwnDocument` in `DocumentAuthorizationTest`
- [ ] All 5 tests in `DocumentAuthorizationTest` pass

