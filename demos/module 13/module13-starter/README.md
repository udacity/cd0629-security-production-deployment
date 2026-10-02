# TaskFlow – Module 13 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security,
CORS and security headers) with new endpoints that contain **deliberate OWASP
vulnerabilities**. None of the mitigations for them are applied yet.

> **Run this project only on your own machine.** It is intentionally insecure.
> Don't deploy it anywhere.

Use this project as the starting point for fixing the vulnerabilities below.

## The vulnerabilities

| OWASP risk | Where | The problem |
|------------|-------|-------------|
| Injection | `AccountService.searchByNameVulnerable` | The JPQL query is built by string concatenation |
| Insecure Design | `AccountController` | Returns the raw `Account` entity, including `internalRiskScore` |
| Security Misconfiguration | `application.yml` | `management.endpoints.web.exposure.include: "*"` exposes every actuator endpoint |
| Cross-Site Scripting (XSS) | `ReportController` | Task titles are written into HTML without encoding |
| Server-Side Request Forgery (SSRF) | `PartnerController` | The server fetches any URL it is given |
| Vulnerable and Outdated Components | `pom.xml` | Nothing scans dependencies for known CVEs |

Two other risks are already handled in earlier modules:
**Broken Access Control** (role hierarchy and the `@PreAuthorize` ownership check)
and **Identification and Authentication Failures** (Keycloak and JWT).

## Tech stack

- Java 25
- Spring Boot 4.0.0 (Spring Security 7, Spring Data JPA, Actuator)
- H2 in-memory database
- OWASP Java Encoder (already a dependency, unused until you fix the XSS issue)
- Maven
- Docker (Keycloak)

## Run it

```bash
mvn spring-boot:run
```

The API starts on <http://localhost:8080>. An H2 database is created in memory at
startup and seeded from `src/main/resources/data.sql`:

| Account | Balance | Internal risk score |
|---------|---------|---------------------|
| `dev1` | 500.00 | 10 |
| `manager1` | 10,000.00 | 95 |
| `admin1` | 250,000.00 | 99 |

Only `POST /api/v1/tasks` needs a token. Start Keycloak with
`docker compose up -d` and use the Module 9 setup if you want to create tasks.

## Try the attacks

**1. Injection and Insecure Design.** A normal search returns one account:

```bash
curl "http://localhost:8080/api/v1/accounts/search?name=dev1"
```

The response already includes `internalRiskScore`, a field that should never be
visible. Now inject a condition that is always true:

```bash
curl -G "http://localhost:8080/api/v1/accounts/search" \
  --data-urlencode "name=dev1' OR '1'='1"
```

All three accounts come back, including `admin1`'s balance and risk score.

**2. Security Misconfiguration.** Read the server's environment with no login:

```bash
curl http://localhost:8080/actuator/env
```

**3. XSS.** Create a task with a script in its title (needs a token), then open
the report page:

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"<script>alert(1)</script>"}'
```

Open <http://localhost:8080/api/v1/tasks/report> and use **View Page Source**. The
raw `<script>` tag is in the HTML, unescaped. The Content-Security-Policy header
from Module 11 (`script-src 'self'`) may stop a browser from running it, but the
output is still unsafe. Don't rely on one defence.

**4. SSRF.** The server fetches whatever URL you give it, including addresses only
the server can reach, such as a cloud metadata endpoint:

```bash
curl "http://localhost:8080/api/v1/partners/fetch?url=http://169.254.169.254/latest/meta-data/"
```

(Outside a cloud machine this request just fails to connect, but nothing stops
the server from trying.)

**5. Vulnerable components.** Open `pom.xml` and confirm there is no
`dependency-check-maven` plugin.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── config/SecurityConfig.java            # JWT, role hierarchy, CORS, security headers
│   ├── controller/
│   │   ├── TaskController.java
│   │   ├── AccountController.java            # Injection and Insecure Design
│   │   ├── ReportController.java             # XSS
│   │   └── PartnerController.java            # SSRF
│   ├── service/
│   │   ├── TaskService.java
│   │   └── AccountService.java               # Vulnerable query
│   ├── repository/AccountRepository.java     # Includes a safe parameterized query
│   ├── model/                                # Task, Account
│   ├── dto/AccountDTO.java                   # Response shape without the risk score
│   ├── security/                             # Retired classes, kept for reference
│   └── util/PasswordHashGenerator.java
└── resources/
    ├── application.yml                       # H2, JPA, actuator exposure
    ├── data.sql                              # Seed accounts
    └── static/demo-trigger.html
```

## Notes

- The H2 console is enabled in `application.yml`. Its page uses frames, which
  may clash with the `X-Frame-Options: DENY` header from Module 11 and render
  blank. That is expected.
- Tasks and accounts live in memory and are reset on restart.
