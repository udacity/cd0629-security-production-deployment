# TaskFlow – Module 25 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs in Elasticsearch and Kibana, and Prometheus and Grafana
monitoring). It has **no Dockerfile**: the app has only ever run on a developer's
machine, with `mvn spring-boot:run`.

Use this project as the starting point for packaging TaskFlow as a Docker image.

## The problem

Running only from source works on one machine, and nowhere else is guaranteed. The
classic "works on my machine" failures come from differences between a laptop and a
server: a different Java version, a missing library, different settings. Without an
image, there's nothing to build once and run unchanged everywhere.

Until now, TaskFlow ran natively while its dependencies (Keycloak, Vault,
Elasticsearch, Prometheus and so on) ran in Docker. TaskFlow itself isn't in a
container.

## What's already in place

- `docker-compose.yml` defines Keycloak, Vault, Elasticsearch, Kibana, Prometheus and
  Grafana, but not TaskFlow
- `prometheus.yml` and `alerts.yml` for monitoring
- `scripts/import-logs.sh` for shipping logs to Elasticsearch

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- Micrometer with the Prometheus registry
- H2 in-memory database
- Maven
- Docker

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

## See the problem

Look in the project folder. There's no `Dockerfile` and no `.dockerignore`. There's
also no way to run TaskFlow in a container, or to hand someone one artifact that runs
the same everywhere.

## Project structure

```
.
├── docker-compose.yml                        # Dependencies only, no TaskFlow
├── prometheus.yml
├── alerts.yml
├── scripts/import-logs.sh
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
        ├── application.yml
        ├── data.sql
        └── static/demo-trigger.html
```

## Notes

- Tasks and accounts live in memory and are reset on restart.
