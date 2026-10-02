# TaskFlow – Module 21 Demo Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, and structured JSON logging with correlation IDs). Its logs are
structured, but they exist **only in the console of one terminal**.

Use this project as the starting point for centralized logging with the ELK stack
(Elasticsearch and Kibana).

## The problem

Each request writes structured JSON logs to the console, and every line carries a
`traceId`. That's readable, but the logs are trapped:

- If the app or its container restarts, the logs are gone.
- There's no way to search them except by scrolling the terminal.
- With 50 services, that would be 50 terminals to search by hand.

Logs need to be written to a file, shipped to a central store, and made searchable.

## What's already in place

- `CorrelationIdFilter` adds an `X-Trace-Id` to every request and every log line.
- `logging.structured.format.console: ecs` writes JSON in the Elastic Common Schema.
- `docker-compose.yml` defines Keycloak and Vault. It has no Elasticsearch or Kibana
  yet.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- H2 in-memory database
- Maven
- Docker (Keycloak and Vault)

## Prerequisites

The partner API key comes from Vault, so Vault must be running with the secret stored.
Follow the setup in the Module 15 project:

```bash
docker compose up -d
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=taskflow-root-token
vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
```

The Config Server from Module 17 is optional. The app starts without it.

## Run it

In the same terminal where `VAULT_TOKEN` is exported:

```bash
mvn spring-boot:run
```

## See the problem

Make a few requests, including the partner fetch so there's a richer log line:

```bash
curl -i http://localhost:8080/api/v1/tasks
curl "http://localhost:8080/api/v1/partners/fetch?url=https://trusted-partner.com/data"
```

The partner request itself fails to connect, because that domain doesn't exist for
this project. The log line is still written.

Look at the console. The JSON is structured, but it only exists in this terminal.
Stop the app and it is gone. There's no `logs/` folder and nothing to search.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── filter/CorrelationIdFilter.java       # X-Trace-Id into every log line
│   ├── config/                               # SecurityConfig, PartnerApiKeyProperties, PartnerRequestProperties
│   ├── controller/                           # Task, Account, Report, Config, Partner controllers
│   ├── service/                              # TaskService, AccountService
│   ├── repository/AccountRepository.java
│   ├── model/                                # Task, Account
│   ├── dto/AccountDTO.java
│   ├── security/                             # Retired classes, kept for reference
│   └── util/PasswordHashGenerator.java
└── resources/
    ├── application.yml                       # Console-only structured logging
    ├── data.sql
    └── static/demo-trigger.html
```

## Notes

- Tasks and accounts live in memory and are reset on restart.
