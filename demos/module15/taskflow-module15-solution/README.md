# TaskFlow – Module 15 Demo Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations) that loads its partner API key from
**HashiCorp Vault** instead of a file in the repository.

## How it works

| Concern | Handled by |
|---------|------------|
| Where the secret lives | Vault, at `secret/taskflow`, key `partner.api-key` |
| How the app reads it | Spring Cloud Vault, via `spring.config.import: "vault://"` |
| How the code uses it | `PartnerApiKeyProperties`, a plain `@ConfigurationProperties` class |
| How the app authenticates to Vault | The `VAULT_TOKEN` environment variable |

Spring Cloud Vault fetches `secret/taskflow` at startup, before the rest of the app
initializes, and exposes its entries as ordinary Spring properties.
`partner.api-key` therefore resolves exactly as it did when it was in
`application.yml`.

**`PartnerApiKeyProperties` and the `X-Partner-Api-Key` header in
`PartnerController` use the same code as before.** The code can't tell where the
value comes from, so moving a secret doesn't mean rewriting the classes that use it.

The relevant part of `application.yml`:

```yaml
spring:
  config:
    import: "vault://"
  cloud:
    vault:
      uri: http://localhost:8200
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        default-context: taskflow
```

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 with `spring-cloud-starter-vault-config`
- H2 in-memory database
- Maven
- Docker (Keycloak and a dev-mode Vault)

Spring Cloud needs Spring Boot 4.0.1 or later, which is why Boot is on 4.0.7.

## Prerequisites

- Java 25, Maven and Docker
- The [Vault CLI](https://developer.hashicorp.com/vault/install)

## Set up Vault (one time)

1. Start Vault (and Keycloak) from the project root:
   ```bash
   docker compose up -d
   ```
2. Point the Vault CLI at your local instance:
   ```bash
   export VAULT_ADDR=http://localhost:8200
   export VAULT_TOKEN=taskflow-root-token
   ```
3. Store the secret:
   ```bash
   vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
   ```
   The key is a fake demo value. You can confirm it with
   `vault kv get secret/taskflow`.

The dev-mode Vault keeps everything in memory, so **the secret is lost whenever the
container restarts**. If the app can't find the key, run step 3 again.

## Run the API

In the same terminal where `VAULT_TOKEN` is exported:

```bash
mvn spring-boot:run
```

The app reads `VAULT_TOKEN` at startup. If it isn't set, startup fails.

## Try it

1. **Watch it load the secret.** In the startup logs, look for Spring Cloud Vault
   fetching `secret/taskflow`.
2. **Look for the secret in the repo.** `application.yml` no longer contains
   `partner.api-key`. The key exists only in Vault.
3. **Call the partner endpoint.** Fetch a URL on the allowlist:
   ```bash
   curl -i "http://localhost:8080/api/v1/partners/fetch?url=https://trusted-partner.com/data"
   ```
   The request fails to connect because that domain doesn't exist for this project.
   What matters is that it gets past the allowlist and attaches the key from Vault.
4. **Break it on purpose.** Stop the app, change the secret with
   `vault kv put secret/taskflow partner.api-key=something-else`, and start the app
   again. The new value is picked up with no code change.

## Project structure

```
.
├── docker-compose.yml                        # Keycloak (8081) and dev-mode Vault (8200)
├── pom.xml                                   # Spring Boot 4.0.7, Spring Cloud BOM, Vault starter
└── src/main/
    ├── java/com/taskflow/
    │   ├── config/
    │   │   ├── SecurityConfig.java           # JWT, role hierarchy, CORS, security headers
    │   │   └── PartnerApiKeyProperties.java  # Same class whether the key is in a file or Vault
    │   ├── controller/
    │   │   ├── TaskController.java
    │   │   ├── AccountController.java
    │   │   ├── ReportController.java
    │   │   └── PartnerController.java        # Sends the key as X-Partner-Api-Key
    │   ├── service/                          # TaskService, AccountService
    │   ├── repository/AccountRepository.java
    │   ├── model/                            # Task, Account
    │   ├── dto/AccountDTO.java
    │   ├── security/                         # Retired classes, kept for reference
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml                   # Vault import and connection settings
        ├── data.sql
        └── static/demo-trigger.html
```

## What this doesn't cover

Vault can do more than static key-value secrets, and this project doesn't use it:

- **Dynamic, leased database credentials** that expire automatically. They need a
  real target database, and H2 has no Vault plugin for this.
- **Credential rotation without a restart**, using `@RefreshScope` and
  `/actuator/refresh`. That would mean re-exposing an actuator endpoint that
  Module 13 locked down.

## Notes

- **The static root token is for local development only.** `${VAULT_TOKEN}` only
  moves the secret one level out, from a file to an environment variable. A
  production setup would authenticate with a real Vault auth method such as
  AppRole, Kubernetes service account auth or cloud IAM auth.
- **Don't log secrets.** `PartnerController` logs the key it attaches
  (`Attaching partner API key: ...`) so you can see the value coming from Vault.
  Remove that line in real code, because logs are often stored and shared widely.
- The dev-mode Vault is in-memory and auto-unsealed, with a fixed root token. Never
  run it like this in production.
- Tasks and accounts live in memory and are reset on restart.
