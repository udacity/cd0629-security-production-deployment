# TaskFlow – Module 17 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, and a partner API key loaded from Vault)
that now gets its plain operational settings from a **Spring Cloud Config Server**
backed by a Git repository. A setting can change **without redeploying or restarting**
the app.

## How it works

| Concern | Handled by |
|---------|------------|
| Secrets (`partner.api-key`) | Vault |
| Plain settings (`partner.request.timeout-seconds`) | Config Server, backed by a Git repo |
| Reading both at startup | `spring.config.import: "vault://,optional:configserver:http://localhost:8888"` |
| Reloading a setting without a restart | `@RefreshScope` plus `POST /actuator/refresh` |

The Config Server returns settings from a Git repository, so you get history and
peer review for configuration, the same as for code. TaskFlow asks for config under its
application name, `taskflow-api`, and the server finds `taskflow-api.yml`.

Key points in the code:

- **`@RefreshScope` on `PartnerRequestProperties`** lets the bean be rebuilt on
  refresh. Without it, the value would be read once at startup and cached for the
  life of the app.
- **`optional:` on the Config Server import** lets TaskFlow start even if the Config
  Server is down. That is a convenience for local development. Vault has no
  `optional:`, because the app can't work properly without its API key.
- **The `refresh` actuator endpoint is exposed.** `management.endpoints.web.exposure.include`
  is `health,refresh`, a deliberate, narrow exception to Module 13's lock-down, not a
  return to exposing everything.
- **Secrets and settings stay separate.** The API key needed Vault because it proves
  identity. A timeout is just a number.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault config and Config client)
- Maven
- Docker (Keycloak and Vault)
- Git

## Prerequisites

You run **three things at once** for this module, so plan your terminals:

1. **Vault** (in Docker), set up as in the Module 15 project.
2. **The Config Server**, a separate Spring Boot service on port 8888.
3. **TaskFlow** on port 8080.

The Config Server and its Git-backed config repository are two separate projects, not
part of this one. They are in the course's `config-infrastructure` folder as
`config-server` and `taskflow-config-repo`. They **must sit side by side in the same
parent directory**, because the Config Server looks for `../taskflow-config-repo`.

## Set up

1. **Start Vault** and store the secret (see the Module 15 project):
   ```bash
   docker compose up -d
   export VAULT_ADDR=http://localhost:8200
   export VAULT_TOKEN=taskflow-root-token
   vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
   ```
2. **Check the config repo** is a Git repository with history:
   ```bash
   cd taskflow-config-repo
   git log --oneline
   ```
   You should see one commit: "Initial TaskFlow config: partner request timeout, 5
   seconds."
3. **Start the Config Server** in its own terminal:
   ```bash
   cd config-server
   mvn spring-boot:run
   ```
4. **Check that it serves the config**, before touching TaskFlow:
   <http://localhost:8888/taskflow-api/default>. The JSON should show
   `partner.request.timeout-seconds: 5`, read from the Git repo.
5. **Start TaskFlow**, in a terminal where `VAULT_TOKEN` is exported:
   ```bash
   mvn spring-boot:run
   ```

## Try it

1. **Read the setting.**
   ```bash
   curl http://localhost:8080/api/v1/config/partner-timeout
   ```
   You get `{"timeoutSeconds":5}`, now sourced from Git instead of a local file.
2. **Change it in the repo and commit.** In `taskflow-config-repo`, edit
   `taskflow-api.yml`, set `timeout-seconds` to `30`, and commit. The Config Server
   serves committed content, so an uncommitted edit won't show up.
   ```bash
   git add -A
   git commit -m "Bump partner request timeout to 30 seconds"
   ```
3. **Refresh TaskFlow**, with no restart:
   ```bash
   curl -X POST http://localhost:8080/actuator/refresh
   ```
4. **Read the setting again.** It now returns `{"timeoutSeconds":30}`.

To put it back, change the value to `5` and repeat steps 2 to 4, or run
`git revert HEAD` in the config repo before refreshing.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── config/
│   │   ├── SecurityConfig.java               # JWT, role hierarchy, CORS, security headers
│   │   ├── PartnerApiKeyProperties.java      # Secret, from Vault
│   │   └── PartnerRequestProperties.java     # Plain setting, @RefreshScope
│   ├── controller/
│   │   ├── TaskController.java
│   │   ├── AccountController.java
│   │   ├── ReportController.java
│   │   ├── PartnerController.java
│   │   └── ConfigController.java             # Shows the current timeout
│   ├── service/                              # TaskService, AccountService
│   ├── repository/AccountRepository.java
│   ├── model/                                # Task, Account
│   ├── dto/AccountDTO.java
│   ├── security/                             # Retired classes, kept for reference
│   └── util/PasswordHashGenerator.java
└── resources/
    ├── application.yml                       # Vault and Config Server imports, refresh exposed
    ├── data.sql
    └── static/demo-trigger.html
```

## What this doesn't cover

Refreshing one instance by hand doesn't scale. With many instances you would use
**Spring Cloud Bus**, so a single refresh event reaches every instance. It needs a
message broker, which this project doesn't have.

## Notes

- **Protect the refresh endpoint in real systems.** In this project,
  `anyRequest().permitAll()` means anyone can call `POST /actuator/refresh`. A
  production setup should require authentication, or restrict it to internal
  traffic.
- The Config Server reads from a local folder (`file://.../taskflow-config-repo`).
  In practice it would point at a remote Git repository.
- The Vault token is a static dev-mode token, for local development only. See the
  Module 15 project.
- Tasks and accounts live in memory and are reset on restart.
