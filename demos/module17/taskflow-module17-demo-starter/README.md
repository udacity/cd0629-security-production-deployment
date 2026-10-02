# TaskFlow – Module 17 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, and a partner API key loaded from Vault).
It also has a plain operational setting, a partner request timeout, that is
**hardcoded in a local file**.

Use this project as the starting point for moving that setting into a central
configuration server.

## The problem

```yaml
partner:
  request:
    timeout-seconds: 5
```

This is a non-secret setting, so keeping it in a local file isn't a security issue.
The problem is operational: changing it means editing the file, rebuilding or
redeploying, and restarting the app. Across 50 service instances, that means
editing and redeploying all 50 by hand.

## Secrets and settings are different

The project has two partner-related values, and they belong in different places:

| Value | Kind | Where it lives | Why |
|-------|------|----------------|-----|
| `partner.api-key` | Secret | Vault (Module 15) | It proves identity, so it must be protected |
| `partner.request.timeout-seconds` | Plain setting | `application.yml` (for now) | It's just a number, a good fit for versioned central config |

Don't mix the two categories. A Config Server stores settings, and Vault stores
secrets.

## Where the setting is used

- `PartnerRequestProperties` is a `@ConfigurationProperties(prefix = "partner.request")`
  class holding `timeoutSeconds`.
- `ConfigController` exposes the current value so you can see it change:
  ```
  GET http://localhost:8080/api/v1/config/partner-timeout
  ```

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2
- H2 in-memory database
- Maven
- Docker (Keycloak and Vault)

## Prerequisites

The partner API key comes from Vault, so Vault must be running with the secret
stored. Follow the setup in the Module 15 project:

```bash
docker compose up -d
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=taskflow-root-token
vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
```

## Run it

In the same terminal where `VAULT_TOKEN` is exported:

```bash
mvn spring-boot:run
```

Then check the setting:

```bash
curl http://localhost:8080/api/v1/config/partner-timeout
```

You should get `{"timeoutSeconds":5}`.

Now try changing it. Edit `timeout-seconds` in `application.yml`, then call the
endpoint again. **The value doesn't change until you restart the app.** That is the
problem the next module fixes.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── config/
│   │   ├── SecurityConfig.java               # JWT, role hierarchy, CORS, security headers
│   │   ├── PartnerApiKeyProperties.java      # Secret, from Vault
│   │   └── PartnerRequestProperties.java     # Plain setting, hardcoded in application.yml
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
    ├── application.yml                       # Vault import, hardcoded timeout
    ├── data.sql
    └── static/demo-trigger.html
```

## Notes

- Tasks and accounts live in memory and are reset on restart.
