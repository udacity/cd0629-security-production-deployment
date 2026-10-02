# TaskFlow – Module 13 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security,
CORS and security headers) with mitigations for six OWASP Top 10 risks.

## Mitigations

| OWASP risk | Where | The fix |
|------------|-------|---------|
| Injection | `AccountRepository.findByNameSafe` | A parameterized `@Query`. `:name` is bound separately from the query text |
| Insecure Design | `AccountDTO`, `AccountService.toDTO` | The API returns a DTO without `internalRiskScore`, so the field can't leak |
| Security Misconfiguration | `application.yml` | Only the `health` actuator endpoint is exposed |
| Cross-Site Scripting (XSS) | `ReportController` | Task titles pass through `Encode.forHtml(...)` |
| Server-Side Request Forgery (SSRF) | `PartnerController` | Outgoing requests are limited to an allowlist of hosts |
| Vulnerable and Outdated Components | `pom.xml` | The OWASP `dependency-check-maven` plugin scans dependencies for known CVEs |

Two other risks are already handled in earlier modules:
**Broken Access Control** (role hierarchy and the `@PreAuthorize` ownership check)
and **Identification and Authentication Failures** (Keycloak and JWT).

### How each fix works

- **Injection:** a user-supplied value is bound as a parameter, so it can never
  change the shape of the query. The old string-concatenating method,
  `searchByNameVulnerable`, is still in `AccountService`, marked retired and
  unused, so you can compare the two.
- **Insecure Design:** an entity can contain fields that should never cross the API
  boundary. Mapping to a DTO means the field is absent from the response by design,
  not by remembering to leave it out.
- **Security Misconfiguration:** `management.endpoints.web.exposure.include: health`
  leaves out `env`, `beans` and the rest. `/actuator/health` stays available.
- **XSS:** `Encode.forHtml` (OWASP Java Encoder) turns `<script>` into
  `&lt;script&gt;`, so it shows as text instead of running.
- **SSRF:** `PartnerController` checks the URL's host against `trusted-partner.com`
  and `api-udapay.com` before making any request, and returns `400` otherwise.
- **Vulnerable components:** the plugin runs during `mvn verify` and writes a report
  to `target/dependency-check-report.html`.

## Tech stack

- Java 25
- Spring Boot 4.0.0 (Spring Security 7, Spring Data JPA, Actuator)
- H2 in-memory database
- OWASP Java Encoder and OWASP Dependency-Check
- Maven
- Docker (Keycloak)

## Run it

```bash
mvn spring-boot:run
```

The API starts on <http://localhost:8080>. An H2 database is created in memory and
seeded from `src/main/resources/data.sql` with three accounts (`dev1`, `manager1`,
`admin1`).

Only `POST /api/v1/tasks` needs a token. Start Keycloak with `docker compose up -d`
and use the Module 9 setup if you want to create tasks.

## Try it

**Injection.** A normal search works, and the injection payload finds nothing:

```bash
curl "http://localhost:8080/api/v1/accounts/search?name=dev1"

curl -G "http://localhost:8080/api/v1/accounts/search" \
  --data-urlencode "name=dev1' OR '1'='1"
```

The first returns only `{id, name, balance}`, with no `internalRiskScore`. The
second returns `[]`, because no account is named that whole string.

**Security Misconfiguration.**

```bash
curl -i http://localhost:8080/actuator/env      # 404
curl -i http://localhost:8080/actuator/health   # 200
```

**XSS.** Create a task titled `<script>alert(1)</script>` (needs a token), then open
<http://localhost:8080/api/v1/tasks/report> and use **View Page Source**. The title
appears as escaped text (`&lt;script&gt;`), not live markup.

**SSRF.**

```bash
# Blocked: 400 Bad Request, host not on the allowlist
curl -i "http://localhost:8080/api/v1/partners/fetch?url=http://169.254.169.254/latest/meta-data/"
```

A URL on the allowlist gets past the check. The request to `trusted-partner.com`
will still fail to connect, because that domain doesn't exist for this project.
What matters is that it isn't rejected by the allowlist.

**Vulnerable components.** See the next section.

## Run the dependency scan

```bash
mvn verify
```

**The first run is slow.** The plugin downloads the whole National Vulnerability
Database, which can take from several minutes to over an hour, especially without an
API key. Later runs use the local cache and finish much faster.

Get a free key at <https://nvd.nist.gov/developers/request-an-api-key> and set it as
an environment variable before running. `pom.xml` reads it as `NVD_API_KEY`:

```bash
export NVD_API_KEY=your-key-here
mvn verify
```

When the scan finishes, open `target/dependency-check-report.html`.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── config/SecurityConfig.java            # JWT, role hierarchy, CORS, security headers
│   ├── controller/
│   │   ├── TaskController.java
│   │   ├── AccountController.java            # Parameterized search, returns AccountDTO
│   │   ├── ReportController.java             # HTML-encoded task titles
│   │   └── PartnerController.java            # Host allowlist
│   ├── service/
│   │   ├── TaskService.java
│   │   └── AccountService.java               # searchByNameSafe, plus the retired vulnerable method
│   ├── repository/AccountRepository.java     # @Query with a bound :name parameter
│   ├── model/                                # Task, Account
│   ├── dto/AccountDTO.java                   # id, name, balance only
│   ├── security/                             # Retired classes, kept for reference
│   └── util/PasswordHashGenerator.java
└── resources/
    ├── application.yml                       # H2, JPA, actuator limited to health
    ├── data.sql                              # Seed accounts
    └── static/demo-trigger.html
```

## Notes

- Three more risks get only a brief mention here, because later modules cover them
  in depth: secrets and cryptographic failures, and logging and monitoring failures.
- The H2 console is enabled in `application.yml`. Its page uses frames, which may
  clash with the `X-Frame-Options: DENY` header from Module 11 and render blank. That
  is expected.
- The SSRF allowlist is hardcoded for this example. In a real application, load it
  from configuration and also consider blocking redirects and private IP ranges.
- Tasks and accounts live in memory and are reset on restart.
