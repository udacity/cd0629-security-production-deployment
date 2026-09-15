# Exercise: Apply OWASP Mitigations in a Spring Boot Application

**Estimated time:** 10–20 minutes

## The scenario

You're a backend engineer at **Udabank**. Remember the compliance review
from a while back (fixing `/app/**` authentication)? That one went fine —
but Priya just forwarded you something new:

> "Good and bad news. Good: [BigCorp Treasury] is ready to sign as a
> partner. Bad: their security team made a full penetration test a
> condition of signing, and the report just landed in my inbox. Most of it
> is about the internal support-lookup tool — the one agents use to search
> customer accounts and leave notes.
>
> The good news inside the bad news: the access-control finding is
> already fixed — you'll see `CustomerService` already checks that an
> agent can only pull up their *own* assigned customers, same pattern
> we've used elsewhere. That one's done.
>
> Still open:
> 1. **SQL injection** in the customer search endpoint.
> 2. **Stored XSS** in the support-notes view — a note contains a
>    `<script>` tag from what looks like a pentester's own probe.
>
> There's also a flagged outdated dependency, but that one's more of a
> 'run the scanner and read the report' thing than a coding task — I put
> together a walkthrough for that separately. Can you get the two code
> fixes done today?"

You pull up `authdemo`. Authentication, the access-control fix, and all
the controllers already exist — the two open findings are contained to one
file each.

## Your tasks

1. [`CustomerSearchService.java`](src/main/java/com/udabank/authdemo/customer/CustomerSearchService.java) —
   `searchByName(name)` builds its SQL query by string concatenation. Fix
   it to use a parameterized query instead. The class-level comment has
   the exact API to use.
2. [`customer-notes.html`](src/main/resources/templates/customer-notes.html) —
   one line renders a note's body with `th:utext` (raw, unescaped HTML)
   instead of `th:text` (which auto-escapes). Fix that one line.

## Running it

```bash
./mvnw spring-boot:run
```

## Verifying your work

```bash
# (1) SQL injection — right now, this classic payload bypasses the name
# filter entirely and returns every customer instead of zero:
curl -u marcus:QueueZero!2025 -G "http://localhost:8080/api/customers/search" \
  --data-urlencode "name=x' OR '1'='1' --"
# Currently: both seeded customers. After your fix: []

# Sanity check — a normal search should still work after your fix:
curl -u marcus:QueueZero!2025 -G "http://localhost:8080/api/customers/search" \
  --data-urlencode "name=Ridgeline"

# (2) Stored XSS — customer 2's notes page currently renders a raw
# <script> tag straight into the HTML:
curl -u sana:TicketDesk!2025 http://localhost:8080/support/customers/2
# Currently: literal <script>...</script> in the response body.
# After your fix: the escaped form, &lt;script&gt;...&lt;/script&gt;
```

Or run the test skeletons — 2 are given as worked examples (they verify
the access-control fix, which is already done), 2 verify your two TODOs:

```bash
./mvnw test -Dtest=OwaspFindingsTest
```

## Task List

- [ ] Parameterize the query in `CustomerSearchService.searchByName`
- [ ] Change `th:utext` to `th:text` in `customer-notes.html`
- [ ] Fill in `searchWithSqlInjectionPayloadReturnsNoResults` in `OwaspFindingsTest`
- [ ] Fill in `xssPayloadInNoteIsEscapedInRenderedHtml` in `OwaspFindingsTest`
- [ ] All 4 tests in `OwaspFindingsTest` pass

## Free Response

In 50–100 words: the SQL injection and the XSS bug both come from the same
underlying mistake, just in different layers (a database query vs. an HTML
template). What's the one-sentence version of that shared mistake? And why
doesn't "just remember to escape/parameterize everything" fully solve it
at Udabank's scale — what would you actually want in place so a future
engineer can't reintroduce either bug without someone noticing?

*(A good reflection response is between 50 and 100 words.)*
