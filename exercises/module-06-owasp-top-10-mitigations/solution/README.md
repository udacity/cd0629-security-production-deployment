# Solution: Apply OWASP Mitigations in a Spring Boot Application

**Scenario recap:** A pre-signing pentest against Udabank's internal
support-lookup tool found SQL injection, stored XSS, and (already fixed by
the time you got the report) broken access control. See
[`../starter/README.md`](../starter/README.md) for Priya's full message
and the task breakdown.

## What changed

[`CustomerSearchService.java`](src/main/java/com/udabank/authdemo/customer/CustomerSearchService.java) —
the query went from string concatenation:

```java
String sql = "... WHERE full_name LIKE '%" + name + "%'";
jdbcTemplate.query(sql, ...);
```

to a parameterized query:

```java
String sql = "... WHERE full_name LIKE ?";
jdbcTemplate.query(sql, mapRow, "%" + name + "%");
```

The search term is now always sent to the database as a bound parameter,
never spliced into the SQL text — so a value like `x' OR '1'='1' --` is
just a (non-matching) string to search for, not executable SQL.

[`customer-notes.html`](src/main/resources/templates/customer-notes.html) —
one attribute change: `th:utext="${note.body}"` → `th:text="${note.body}"`.
Thymeleaf's `th:text` HTML-escapes its value automatically; `th:utext`
explicitly opts out of that, which is exactly what you want for content
you generated yourself (a nonce'd inline script, say) and exactly what you
don't want for text a customer typed into a chat widget.

## Solution walkthrough: running OWASP Dependency-Check

This is the one part of this exercise I couldn't turn into a clean "run
this and get a report in 30 seconds" step, and I want to be honest about
why rather than paper over it.

`pom.xml` already has the plugin declared (not bound to a build phase —
see the comment there for why). Running it for real:

```bash
mvn org.owasp:dependency-check-maven:check
```

I actually ran this while building the exercise. Here's what happened,
verbatim:

```
[INFO] Checking for updates
[WARNING] An NVD API Key was not provided - it is highly recommended to use
an NVD API key as the update can take a VERY long time without an API Key
[INFO] NVD API has 362,830 records in this update
[INFO] Downloaded 10,000/362,830 (3%)
[INFO] Downloaded 20,000/362,830 (6%)
```

...at which point I stopped it after several minutes, at 6% of the initial
NVD data sync. That warning about needing an API key isn't boilerplate —
it's the actual bottleneck. **For real use** (not this exercise): get a
free key at nvd.nist.gov/developers/request-an-api-key and pass it with
`-DnvdApiKey=$NVD_API_KEY` (or configure it in the plugin's `<properties>`
in `pom.xml`) — with a key, the initial sync is a much shorter one-time
cost, and every run after that is an incremental update, not a full resync.

**What the report would flag here, once it finishes:** `commons-text 1.9`
against `CVE-2022-42889` ("Text4Shell") — a real, published RCE-class
vulnerability in Apache Commons Text's string interpolation feature
(`StringSubstitutor`) affecting versions before 1.10.0. The fix in this
solution's `pom.xml` is exactly what the report's remediation advice would
say: bump to `1.10.0` or later.

**How to read a dependency-check report once you have one:** it generates
`target/dependency-check-report.html` (and a machine-readable
`dependency-check-report.json`). For each flagged dependency you'll see
the CVE ID, a CVSS severity score, and a plain-language description —
triage by severity first, but also sanity-check *reachability*: a critical
CVE in a transitive dependency whose vulnerable code path your app never
calls is a lower real-world priority than a moderate one in code you
actually invoke directly.

## Verifying the two code fixes

```bash
./mvnw test -Dtest=OwaspFindingsTest
# 4 tests, all green
```

```bash
./mvnw spring-boot:run
```

```bash
curl -u marcus:QueueZero!2025 -G "http://localhost:8080/api/customers/search" \
  --data-urlencode "name=x' OR '1'='1' --"
# []

curl -u sana:TicketDesk!2025 http://localhost:8080/support/customers/2
# ...&lt;script&gt;document.location=&#39;https://evil-lookalike.example/...
```

## Free Response — one answer

Both bugs are the same mistake wearing different clothes: **untrusted
input was written directly into a language a downstream system would
interpret** — SQL for the database, HTML for the browser — instead of
going through an API that treats it strictly as data. "Remember to escape
everything" doesn't scale because it depends on every engineer, on every
change, forever, never forgetting one call site — exactly the kind of
thing a pentest exists to catch *after* it's already shipped. What
actually holds at scale: make the safe path the *only* path (a lint rule
or code-review checklist banning raw `JdbcTemplate` string concatenation
and `th:utext` outside of explicitly reviewed exceptions), plus automated
scanning in CI (static analysis for injection patterns, Dependency-Check
for known-vulnerable libraries) so a reintroduced bug gets caught by a
pipeline, not by the next external pentest.

## Common pitfalls

- **Testing the SQL injection fix by checking the query "looks
  parameterized," not by actually sending a payload.** The real test is
  behavioral: send `x' OR '1'='1' --` and assert on the *result* (empty),
  not on the source code. A refactor that keeps the vulnerability but
  changes the code's shape would pass a code-review-only check and fail
  the behavioral one — the behavioral one is the one that matters.
- **Fixing XSS by escaping at write-time instead of render-time.** It's
  tempting to "clean" the note body before saving it to the database.
  Don't — you'll eventually have a second view of the same data that
  renders it differently (plain text export, an API response, a mobile
  app), and each one needs its own context-appropriate escaping. Thymeleaf
  escaping at render time (`th:text`) means the stored data stays exactly
  what the customer typed, and every consumer decides how to safely
  display it.
