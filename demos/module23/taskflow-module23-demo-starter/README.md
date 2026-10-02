# TaskFlow – Module 23 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, and structured logs shipped to Elasticsearch and Kibana). It has **no
metrics monitoring**, and it has a security gap: `/actuator/refresh` is open to
everyone.

Use this project as the starting point for adding application monitoring with
Prometheus and Grafana, and for closing the actuator gap.

## The problems

### No metrics

Logs tell you what happened. They don't show you how the app is performing right now.
Without metrics you can't see request rates, latency percentiles or business counts.
`/actuator/health` returns a flat `{"status":"UP"}`: the process is alive, but nothing
says whether users are having a good time. A green status can sit next to a slow,
broken experience.

### An open reload endpoint

`/actuator/refresh` (added in Module 17) triggers a live configuration reload. It is
reachable by anyone with no authentication, because the catch-all
`anyRequest().permitAll()` rule covers it.

## What's already in place

- Structured JSON logs with correlation IDs, written to `logs/taskflow.log`
- Elasticsearch and Kibana in `docker-compose.yml`, plus `scripts/import-logs.sh`
- The actuator exposes `health` and `refresh` only

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- H2 in-memory database
- Maven
- Docker (Keycloak, Vault, Elasticsearch and Kibana)

## Prerequisites

The partner API key comes from Vault, so Vault must be running with the secret stored.
Follow the setup in the Module 15 project:

```bash
docker compose up -d
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=taskflow-root-token
vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
```

Docker Desktop should have at least 4 GB of memory, because Elasticsearch is included.
The Config Server from Module 17 is optional.

## Run it

In the same terminal where `VAULT_TOKEN` is exported:

```bash
mvn spring-boot:run
```

## See the problems

**1. The open endpoint.** Trigger a configuration reload with no credentials:

```bash
curl -i -X POST http://localhost:8080/actuator/refresh
```

It succeeds. Anyone who can reach the app can force a live config reload.

**2. The flat health check.**

```bash
curl http://localhost:8080/actuator/health
```

You get `{"status":"UP"}`: binary, with no detail about latency or errors.

**3. No metrics endpoint.**

```bash
curl -i http://localhost:8080/actuator/prometheus
```

It isn't exposed, so there's nothing for Prometheus to scrape.

## Project structure

```
.
├── docker-compose.yml                        # Keycloak, Vault, Elasticsearch, Kibana
├── scripts/import-logs.sh                    # Ships the log file into Elasticsearch
├── pom.xml
└── src/main/
    ├── java/com/taskflow/
    │   ├── filter/CorrelationIdFilter.java
    │   ├── config/                           # SecurityConfig, PartnerApiKeyProperties, PartnerRequestProperties
    │   ├── controller/                       # Task, Account, Report, Config, Partner controllers
    │   ├── service/                          # TaskService, AccountService
    │   ├── repository/AccountRepository.java
    │   ├── model/                            # Task, Account
    │   ├── dto/AccountDTO.java
    │   ├── security/                         # Retired classes, kept for reference
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml                   # Actuator exposes health and refresh
        ├── data.sql
        └── static/demo-trigger.html
```

## Notes

- Tasks and accounts live in memory and are reset on restart.
