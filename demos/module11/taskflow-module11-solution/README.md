# TaskFlow – Module 11 Solution

A Spring Boot REST API secured as an OAuth2 Resource Server (Keycloak-issued
JWTs, role hierarchy, method security), with an explicit **CORS allowlist** and a
full set of **security response headers**.

The project includes `cors-frontend/`, a small static site served from a
different origin. It lets you test CORS for real and try a simulated clickjacking
attack.

## What's configured

All of it lives in `SecurityConfig`.

### CORS

`corsConfigurationSource()` allows exactly one origin, `http://localhost:3000`,
and is wired in with `.cors(Customizer.withDefaults())`.

| Setting | Value |
|---------|-------|
| Allowed origins | `http://localhost:3000` (an explicit allowlist, never `*`) |
| Allowed methods | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Allowed headers | `Authorization`, `Content-Type` |

A wildcard would defeat CORS as a trust boundary, so each trusted origin is
listed by name.

### Security headers

| Header | Value | Protects against |
|--------|-------|------------------|
| `X-Frame-Options` | `DENY` | Clickjacking |
| `Content-Security-Policy` | `frame-ancestors 'none'; script-src 'self'` | Framing and injected scripts |
| `Strict-Transport-Security` | 1 year, `includeSubDomains` | Downgrade to plain HTTP |
| `X-Content-Type-Options` | `nosniff` | MIME-type sniffing |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Leaking URLs to other sites |
| `Cross-Origin-Opener-Policy` | `same-origin` | Cross-window attacks |
| `Cross-Origin-Embedder-Policy` | `require-corp` | Loading unvetted cross-origin resources |
| `Cross-Origin-Resource-Policy` | `same-origin` | Other sites embedding your resources |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` | Unneeded browser features |

A few things to know when you test:

- **`X-Frame-Options` and `X-Content-Type-Options` aren't new.** Spring Security
  sends both by default, with no configuration. They're written out explicitly so
  the protection can't be lost silently if someone later calls
  `.headers(headers -> headers.defaultsDisabled())`.
- **`Strict-Transport-Security` won't show up locally.** Browsers only receive it
  over HTTPS, and `http://localhost` never triggers it.
- **`Permissions-Policy` uses a `StaticHeadersWriter`.** Spring's servlet stack has
  no dedicated DSL method for it yet.
- **CSRF stays disabled.** The API is stateless and has no cookies or session for a
  forged request to ride on.

## Tech stack

- Java 25
- Spring Boot 4.0.0 (Spring Security 7, lambda DSL only)
- Maven
- Docker (Keycloak)
- Python 3 (to serve the test frontend)

## Run it

**1. Start the API:**

```bash
mvn spring-boot:run
```

It runs on <http://localhost:8080>. `GET /api/v1/tasks` is public, so you don't
need a token for the tests below.

**2. Serve the test frontend** in a second terminal:

```bash
cd cors-frontend
python3 -m http.server 3000
```

**3. (Optional) Start Keycloak** to call the protected endpoints:

```bash
docker compose up -d
```

Keycloak runs on <http://localhost:8081> with the `admin` / `admin` login. See
the Module 9 project for the realm, client and user setup.

## Try it

1. **CORS works.** Open <http://localhost:3000> and click **Fetch Tasks from
   localhost:8080**. The request succeeds. In DevTools, open the **Network** tab
   and check that the response has `Access-Control-Allow-Origin: http://localhost:3000`.
2. **Check the headers.** In DevTools, load `http://localhost:8080/api/v1/tasks`
   and inspect the response headers. You should see the headers from the table
   above, except `Strict-Transport-Security`.
3. **Clickjacking is blocked.** Open <http://localhost:3000/attacker.html>. The
   iframe doesn't render, and the console shows
   `Refused to display ... X-Frame-Options to 'deny'`.
4. **Other origins are still refused.** Change the origin in
   `corsConfigurationSource()`, restart, and repeat step 1. The request fails
   again, because only listed origins are trusted.

You can also check the headers from a terminal:

```bash
curl -i http://localhost:8080/api/v1/tasks
```

## Project structure

```
.
├── cors-frontend/
│   ├── index.html                        # Fetches /api/v1/tasks from another origin
│   └── attacker.html                     # Embeds the API's demo-trigger.html in an iframe
├── docker-compose.yml                    # Keycloak on port 8081
├── pom.xml
└── src/main/
    ├── java/com/taskflow/
    │   ├── config/SecurityConfig.java    # CORS allowlist, security headers, JWT, role hierarchy
    │   ├── controller/TaskController.java
    │   ├── service/TaskService.java
    │   ├── model/Task.java
    │   ├── security/                     # Retired classes, kept for reference
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml               # Keycloak issuer-uri
        └── static/demo-trigger.html      # Page the attacker iframe tries to embed
```

## Notes

- `localhost:3000` and `localhost:8080` count as different origins because the
  ports differ, so the CORS test is real, not simulated.
- The CORS allowlist is hardcoded to `http://localhost:3000` for local
  development. In a real deployment, load allowed origins from configuration per
  environment.
- Tasks live in memory and are lost on restart.
