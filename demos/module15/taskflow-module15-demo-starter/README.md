# TaskFlow – Module 15 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations) that calls a partner service using an API
key. The key is **hardcoded in plain text in `application.yml`**.

> The key in this project is a fake demo value. Never commit a real secret to a
> repository.

Use this project as the starting point for moving that secret into HashiCorp Vault.

## The problem

```yaml
partner:
  api-key: sk_live_hardcoded_demo_9f8e7d6c5b4a
```

This key sits in a file that is part of the repository. If it were committed and
pushed, it would stay in Git history forever, even after the line is deleted,
because the old commit still contains it. And it isn't a decorative value:
`PartnerController` sends it in an `X-Partner-Api-Key` header on every outgoing
partner request.

## How the key is used

- `PartnerApiKeyProperties` is a `@ConfigurationProperties(prefix = "partner")`
  class that holds the key.
- `PartnerController` reads it and attaches it as the `X-Partner-Api-Key` header.

Neither class knows where the value comes from. Once a value looks like any other
Spring property, the code can't tell whether it came from `application.yml` or a
secrets manager. That is what lets you move it without changing them.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault config starter, included but not used yet)
- H2 in-memory database
- Maven
- Docker (Keycloak and Vault)

Spring Cloud needs Spring Boot 4.0.1 or later, which is why Boot is on 4.0.7 here.

## Run it

```bash
mvn spring-boot:run
```

The API starts on <http://localhost:8080>. No Vault is needed yet, because the key
comes straight from `application.yml`.

`docker-compose.yml` defines Keycloak (port 8081) and a dev-mode Vault (port 8200).
You don't need either to see the problem.

## Look at the problem

1. Open `src/main/resources/application.yml` and find the `partner.api-key` entry.
2. Open `PartnerApiKeyProperties.java`, then the `X-Partner-Api-Key` header in
   `PartnerController.java`. The secret is actively used on every outgoing request.
3. Ask yourself what a `git log` would reveal if this file had ever been pushed.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── config/
│   │   ├── SecurityConfig.java               # JWT, role hierarchy, CORS, security headers
│   │   └── PartnerApiKeyProperties.java      # Holds partner.api-key
│   ├── controller/
│   │   ├── TaskController.java
│   │   ├── AccountController.java
│   │   ├── ReportController.java
│   │   └── PartnerController.java            # Sends the key as X-Partner-Api-Key
│   ├── service/                              # TaskService, AccountService
│   ├── repository/AccountRepository.java
│   ├── model/                                # Task, Account
│   ├── dto/AccountDTO.java
│   ├── security/                             # Retired classes, kept for reference
│   └── util/PasswordHashGenerator.java
└── resources/
    ├── application.yml                       # Contains the hardcoded partner.api-key
    ├── data.sql
    └── static/demo-trigger.html
```

## Notes

- The partner call only goes to hosts on the allowlist from Module 13, so a real
  request to `trusted-partner.com` will fail to connect. That doesn't change the
  point of this module.
- Tasks and accounts live in memory and are reset on restart.
