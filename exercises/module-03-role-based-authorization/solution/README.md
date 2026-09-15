# Solution: Apply Role-Based Authorization

**Scenario recap:** Inkwell had an IDOR (insecure direct object reference)
bug — any authenticated user could view or delete any document by guessing
IDs, because `DocumentService` never checked who was asking. See
[`../starter/README.md`](../starter/README.md) for Jules's full report and
the task breakdown.

## What changed

[`DocumentService.java`](src/main/java/com/inkwell/authdemo/document/DocumentService.java):

```java
@PreAuthorize("hasRole('ADMIN') or @documentSecurity.isOwner(#id, authentication)")
public Document view(Long id) { ... }

@PreAuthorize("hasRole('ADMIN')")
public void delete(Long id) { ... }
```

`@documentSecurity` resolves to the `DocumentSecurity` bean (registered
under that name), whose `isOwner(Long documentId, Authentication authentication)`
does exactly one thing: load the document and compare its `ownerUsername`
to `authentication.getName()`.

## Solution walkthrough: common authorization pitfalls

This is the part of the exercise Jules will actually ask about in code
review — not "does it work," but "what's still fragile." Three real ones,
including one I hit myself while building this exercise:

**1. CSRF protection silently blocking your own API.** The first version
of `SecurityConfig` I wrote for this module didn't disable CSRF, and every
`DELETE` request came back `403` — not from `@PreAuthorize`, from Spring
Security's `CsrfFilter` rejecting the request before it ever reached the
controller. `DocumentController` is a stateless API authenticated with
HTTP Basic on every request, not a browser form flow with a session
cookie — CSRF protection exists for the latter, not the former, so it
needs `.csrf(AbstractHttpConfigurer::disable)`. The failure mode is a trap:
a `403` from CSRF and a `403` from `@PreAuthorize` look *identical* from
the outside, so it's easy to convince yourself your authorization logic
works when you've actually just discovered an unrelated filter. Always
check which filter produced a `403` before declaring victory — this
project's `FilterChainInspector`-style logging (see module 1) is one way;
temporarily loosening `@PreAuthorize` to `permitAll` and re-testing is a
faster gut-check.

**2. Testing `@PreAuthorize` with `@WebMvcTest` + `@MockBean` tests
nothing.** It's tempting to slice-test just `DocumentController` with
`@WebMvcTest` and `@MockBean DocumentService`. Don't — `@MockBean` replaces
the *entire* Spring-managed bean with a Mockito mock, and Spring's method
security works by wrapping the real bean in an AOP proxy. A mock was never
proxied, so `@PreAuthorize` never runs, and your "security test" passes
even if you delete the annotation entirely. This exercise uses a full
`@SpringBootTest` with a real `DocumentService` bean for exactly this
reason — see `DocumentAuthorizationTest`. If you need a lighter-weight
test, test `DocumentService` directly (still as a real Spring bean via
`@SpringBootTest` or a method-security-specific test slice), not through a
mocked layer that sits behind the checked method.

**3. Checking ownership in the controller, after the fact.** A version
that "looks right" in a quick manual test: load the document in the
controller, compare `document.getOwnerUsername()` to
`authentication.getName()`, and return 403 if it doesn't match — but only
on the `view` endpoint, because that's the one Jules mentioned. It'll pass
a manual click-through. It won't stop the next developer from adding
`GET /api/documents/{id}/export` or `PATCH /api/documents/{id}` without
remembering to copy-paste the same check — because the check lives in the
controller layer, once per endpoint, instead of once on the method that
every endpoint eventually calls. That's the actual reason this fix lives
on `DocumentService.view`/`delete` via `@PreAuthorize` rather than in
`DocumentController`: new callers inherit the protection automatically
instead of needing to remember it.

## Verifying the solution

```bash
./mvnw test -Dtest=DocumentAuthorizationTest
# 5 tests, all green
```

```bash
./mvnw spring-boot:run
```

```bash
curl -u bob:MyContracts!2025 http://localhost:8080/api/documents/1
# 200 — bob owns document 1

curl -i -u bob:MyContracts!2025 http://localhost:8080/api/documents/2
# 403 — Jules's bug, now fixed

curl -u alice:SupportDesk!2025 http://localhost:8080/api/documents/2
# 200 — admin can see anything

curl -i -u bob:MyContracts!2025 -X DELETE http://localhost:8080/api/documents/1
# 403 — owning it isn't enough to delete it
```
