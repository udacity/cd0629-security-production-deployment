# TaskFlow – Module 11 Starter

A Spring Boot REST API secured as an OAuth2 Resource Server (Keycloak-issued
JWTs, role hierarchy, method security). It has **no CORS configuration and no
explicit security headers** yet.

The project also includes `cors-frontend/`, a small static site you serve from a
different origin than the API. It gives you a real cross-origin request to
test against, and a simulated "attacker" page that tries to embed the API in an
iframe.

Use this project as the starting point for adding CORS and security headers.

## Current behaviour

| Area | State |
|------|-------|
| Authentication | JWT bearer tokens from Keycloak, stateless |
| Authorization | `POST` needs authentication, `DELETE` needs `MANAGER`, `PATCH .../complete` needs ownership |
| CORS | Not configured, so browsers block cross-origin calls from other sites |
| Security headers | Only Spring Security's defaults, with no `.headers(...)` block |

## What you will observe

- **CORS error:** a page on `http://localhost:3000` can't read responses from
  `http://localhost:8080`, because the API doesn't say it trusts that origin.
- **Clickjacking is already blocked.** Spring Security sends
  `X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff` by default,
  with no configuration. The attacker page's iframe won't render, even in this
  unmodified project.
- **Missing headers:** none of these appear in the response yet:
  - `Content-Security-Policy`
  - `Referrer-Policy`
  - `Cross-Origin-Opener-Policy`
  - `Cross-Origin-Embedder-Policy`
  - `Cross-Origin-Resource-Policy`
  - `Permissions-Policy`

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

1. **See the CORS error.** Open <http://localhost:3000> and click
   **Fetch Tasks from localhost:8080**. In the browser DevTools console you'll see
   the request blocked by CORS.
2. **See the iframe blocked.** Open <http://localhost:3000/attacker.html>. The
   iframe fails to render, and the console shows
   `Refused to display ... X-Frame-Options to 'deny'`.
3. **Inspect the headers.** In DevTools, open the **Network** tab, load
   `http://localhost:8080/api/v1/tasks`, and check the response headers. Confirm
   the six headers above are missing.

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
    │   ├── config/SecurityConfig.java    # JWT resource server, role hierarchy
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

- The two sites are `localhost:3000` and `localhost:8080`. They count as
  different origins because the ports differ, so the CORS test is real, not
  simulated.
- Tasks live in memory and are lost on restart.
